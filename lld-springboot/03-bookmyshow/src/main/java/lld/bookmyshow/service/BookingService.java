package lld.bookmyshow.service;
import jakarta.transaction.Transactional;
import lld.bookmyshow.dto.*;
import lld.bookmyshow.entity.*;
import lld.bookmyshow.enums.*;
import lld.bookmyshow.exception.BookingException;
import lld.bookmyshow.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class BookingService {
    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private static final Duration LOCK_TTL = Duration.ofMinutes(5);
    private static final Map<SeatTier, Double> TIER_MULTIPLIER = Map.of(
        SeatTier.RECLINER, 2.5, SeatTier.PREMIUM, 1.8, SeatTier.EXECUTIVE, 1.3, SeatTier.NORMAL, 1.0);

    private final ShowRepository    showRepo;
    private final SeatRepository    seatRepo;
    private final BookingRepository bookingRepo;

    @Transactional
    public BookingResponse lockSeats(LockSeatsRequest req) {
        log.info("[BookingService] Lock seats | user={} show={} count={} strategy={}", req.getUserId(), req.getShowId(), req.getSeatCount(), req.getStrategy());
        Show show = showRepo.findById(req.getShowId()).orElseThrow(() -> new BookingException("Show not found: " + req.getShowId(), HttpStatus.NOT_FOUND));
        List<Seat> selected = selectSeats(req, show);
        String bookingId = "BKG-" + UUID.randomUUID().toString().substring(0,8).toUpperCase();
        Instant expiry = Instant.now().plus(LOCK_TTL);
        List<String> seatCodes = new ArrayList<>();
        for (Seat seat : selected) {
            int locked = seatRepo.tryLock(seat.getId(), bookingId, expiry, SeatStatus.LOCKED);
            if (locked == 0) {
                seatRepo.releaseByBookingId(bookingId);
                log.warn("[BookingService] Seat {} taken concurrently, rolling back", seat.getSeatCode());
                throw new BookingException("Seat " + seat.getSeatCode() + " is no longer available", HttpStatus.CONFLICT);
            }
            seatCodes.add(seat.getSeatCode());
        }
        long totalPaise = selected.stream().mapToLong(s -> Math.round(s.getBasePricePaise() * TIER_MULTIPLIER.getOrDefault(s.getTier(), 1.0))).sum();
        Booking booking = new Booking();
        booking.setBookingId(bookingId); booking.setUserId(req.getUserId());
        booking.setShow(show); booking.setSeatIds(selected.stream().map(Seat::getId).collect(Collectors.toList()));
        booking.setTotalPaise(totalPaise); booking.setLockExpiry(expiry);
        bookingRepo.save(booking);
        log.info("[BookingService] Seats locked | bookingId={} seats={} total=Rs.{} expiresIn=5min", bookingId, seatCodes, totalPaise/100.0);
        return BookingResponse.builder().bookingId(bookingId).userId(req.getUserId())
            .movieName(show.getMovieName()).showTime(show.getShowTime().toString())
            .seatCodes(seatCodes).totalAmount(fmt(totalPaise))
            .status(BookingStatus.LOCKED).lockExpiry(expiry).message("Seats locked. Complete payment within 5 minutes.").build();
    }

    @Transactional
    public BookingResponse confirmBooking(String bookingId) {
        log.info("[BookingService] Confirm booking | bookingId={}", bookingId);
        Booking booking = getBooking(bookingId);
        if (booking.getStatus() != BookingStatus.LOCKED)
            throw new BookingException("Booking not in LOCKED state: " + booking.getStatus(), HttpStatus.CONFLICT);
        if (Instant.now().isAfter(booking.getLockExpiry())) {
            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepo.save(booking);
            seatRepo.releaseByBookingId(bookingId);
            throw new BookingException("Booking has expired: " + bookingId, HttpStatus.GONE);
        }
        seatRepo.confirmByBookingId(bookingId);
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepo.save(booking);
        log.info("[BookingService] Booking confirmed | bookingId={} total=Rs.{}", bookingId, booking.getTotalPaise()/100.0);
        return buildResponse(booking, "Booking confirmed successfully");
    }

    @Transactional
    public BookingResponse cancelBooking(String bookingId) {
        log.info("[BookingService] Cancel booking | bookingId={}", bookingId);
        Booking booking = getBooking(bookingId);
        if (booking.getStatus() == BookingStatus.CANCELLED)
            throw new BookingException("Already cancelled: " + bookingId, HttpStatus.CONFLICT);
        seatRepo.releaseByBookingId(bookingId);
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepo.save(booking);
        log.info("[BookingService] Booking cancelled | bookingId={}", bookingId);
        return buildResponse(booking, "Booking cancelled" + (booking.getStatus() == BookingStatus.CONFIRMED ? " - Refund initiated" : ""));
    }

    public ShowAvailabilityResponse getAvailability(Long showId) {
        Show show = showRepo.findById(showId).orElseThrow(() -> new BookingException("Show not found: " + showId, HttpStatus.NOT_FOUND));
        long available = seatRepo.countByShowIdAndStatus(showId, SeatStatus.AVAILABLE);
        Map<String, Long> byTier = new HashMap<>();
        Map<String, String> priceByTier = new HashMap<>();
        for (SeatTier tier : SeatTier.values()) {
            long cnt = seatRepo.findByShowIdAndTierAndStatusOrderByRowNumberAscColNumberAsc(showId, tier, SeatStatus.AVAILABLE).size();
            byTier.put(tier.name(), cnt);
            priceByTier.put(tier.name(), fmt(Math.round(15000 * TIER_MULTIPLIER.get(tier))));
        }
        return ShowAvailabilityResponse.builder().showId(showId).movieName(show.getMovieName())
            .showTime(show.getShowTime().toString()).totalSeats(show.getTotalSeats())
            .availableSeats(available).availableByTier(byTier).priceByTier(priceByTier).build();
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void releaseExpiredLocks() {
        int released = seatRepo.releaseExpiredLocks(Instant.now());
        if (released > 0) log.info("[BookingService] Released {} expired seat locks", released);
    }

    private List<Seat> selectSeats(LockSeatsRequest req, Show show) {
        List<Seat> available;
        if (req.getStrategy() == SeatStrategy.SPECIFIC_TIER && req.getPreferredTier() != null) {
            available = seatRepo.findByShowIdAndTierAndStatusOrderByRowNumberAscColNumberAsc(show.getId(), req.getPreferredTier(), SeatStatus.AVAILABLE);
        } else {
            available = seatRepo.findByShowIdAndStatusOrderByRowNumberAscColNumberAsc(show.getId(), SeatStatus.AVAILABLE);
        }
        if (available.size() < req.getSeatCount())
            throw new BookingException("Not enough seats. Available: " + available.size(), HttpStatus.CONFLICT);
        if (req.getStrategy() == SeatStrategy.GROUP_TOGETHER) {
            Map<Integer, List<Seat>> byRow = available.stream().collect(Collectors.groupingBy(Seat::getRowNumber));
            for (List<Seat> rowSeats : byRow.values()) {
                if (rowSeats.size() >= req.getSeatCount()) return rowSeats.subList(0, req.getSeatCount());
            }
            throw new BookingException("No " + req.getSeatCount() + " consecutive seats in same row", HttpStatus.CONFLICT);
        }
        int mid = available.stream().mapToInt(Seat::getRowNumber).max().orElse(0) / 2;
        return available.stream().sorted(Comparator.comparingInt(s -> Math.abs(s.getRowNumber() - mid))).limit(req.getSeatCount()).collect(Collectors.toList());
    }

    private Booking getBooking(String id) {
        return bookingRepo.findById(id).orElseThrow(() -> new BookingException("Booking not found: " + id, HttpStatus.NOT_FOUND));
    }

    private BookingResponse buildResponse(Booking b, String msg) {
        return BookingResponse.builder().bookingId(b.getBookingId()).userId(b.getUserId())
            .movieName(b.getShow().getMovieName()).showTime(b.getShow().getShowTime().toString())
            .totalAmount(fmt(b.getTotalPaise())).status(b.getStatus())
            .lockExpiry(b.getLockExpiry()).message(msg).build();
    }

    private String fmt(long paise) { return String.format("Rs.%.2f", paise / 100.0); }
}
