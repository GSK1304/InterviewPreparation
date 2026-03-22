package lld.hotel.service;
import jakarta.transaction.Transactional;
import lld.hotel.dto.*;
import lld.hotel.entity.*;
import lld.hotel.enums.*;
import lld.hotel.exception.HotelException;
import lld.hotel.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service @RequiredArgsConstructor
public class HotelService {
    private static final Logger log = LoggerFactory.getLogger(HotelService.class);
    private static final Map<AmenityType, Long> AMENITY_COST = Map.of(AmenityType.BREAKFAST,60000L, AmenityType.PARKING,20000L, AmenityType.SPA,150000L);
    private static final double WEEKEND_SURCHARGE = 1.3;

    private final RoomRepository       roomRepo;
    private final ReservationRepository resRepo;
    private final GuestRepository      guestRepo;

    public List<Room> searchRooms(RoomSearchRequest req) {
        log.info("[HotelService] Search rooms | checkIn={} checkOut={} guests={} type={}", req.getCheckIn(), req.getCheckOut(), req.getGuestCount(), req.getRoomType());
        if (!req.getCheckOut().isAfter(req.getCheckIn())) throw new HotelException("Check-out must be after check-in", HttpStatus.BAD_REQUEST);
        long maxRate = req.getMaxPricePerNight() != null ? Math.round(req.getMaxPricePerNight()*100) : Long.MAX_VALUE;
        List<Room> rooms = roomRepo.findAvailable(req.getCheckIn(), req.getCheckOut(), req.getGuestCount(), req.getRoomType(), req.getBedType(), maxRate);
        log.info("[HotelService] Found {} available rooms", rooms.size());
        return rooms;
    }

    @Transactional
    public synchronized Map<String,Object> bookRoom(BookRoomRequest req) {
        log.info("[HotelService] Book room | room={} guest={} checkIn={} checkOut={}", req.getRoomNumber(), req.getGuestEmail(), req.getCheckIn(), req.getCheckOut());
        if (!req.getCheckOut().isAfter(req.getCheckIn())) throw new HotelException("Check-out must be after check-in", HttpStatus.BAD_REQUEST);
        Room room = roomRepo.findById(req.getRoomNumber()).orElseThrow(() -> new HotelException("Room not found: " + req.getRoomNumber(), HttpStatus.NOT_FOUND));
        if (req.getGuestCount() > room.getCapacity()) throw new HotelException("Guest count " + req.getGuestCount() + " exceeds room capacity " + room.getCapacity(), HttpStatus.BAD_REQUEST);
        if (!resRepo.findConflicting(req.getRoomNumber(), req.getCheckIn(), req.getCheckOut()).isEmpty())
            throw new HotelException("Room " + req.getRoomNumber() + " not available for selected dates", HttpStatus.CONFLICT);

        Guest guest = guestRepo.findByEmail(req.getGuestEmail()).orElseGet(() -> {
            Guest g = new Guest(); g.setName(req.getGuestName()); g.setEmail(req.getGuestEmail()); g.setPhone(req.getGuestPhone()); g.setIdProof(req.getIdProof());
            return guestRepo.save(g);
        });

        long dailyRate = room.getBaseRatePaise();
        if (req.getAmenities() != null) for (AmenityType a : req.getAmenities()) dailyRate += AMENITY_COST.getOrDefault(a, 0L);
        long nights = ChronoUnit.DAYS.between(req.getCheckIn(), req.getCheckOut());
        long totalPaise = 0;
        LocalDate d = req.getCheckIn();
        while (!d.equals(req.getCheckOut())) {
            boolean weekend = d.getDayOfWeek() == DayOfWeek.FRIDAY || d.getDayOfWeek() == DayOfWeek.SATURDAY;
            totalPaise += Math.round(dailyRate * (weekend ? WEEKEND_SURCHARGE : 1.0));
            d = d.plusDays(1);
        }
        HotelReservation res = new HotelReservation();
        res.setReservationId("RES-" + UUID.randomUUID().toString().substring(0,8).toUpperCase());
        res.setGuest(guest); res.setRoom(room); res.setCheckIn(req.getCheckIn()); res.setCheckOut(req.getCheckOut());
        res.setGuestCount(req.getGuestCount()); res.setTotalPaise(totalPaise);
        if (req.getAmenities() != null) res.setAmenities(req.getAmenities());
        resRepo.save(res);
        log.info("[HotelService] Reservation created | id={} room={} nights={} total=Rs.{}", res.getReservationId(), req.getRoomNumber(), nights, totalPaise/100.0);
        return Map.of("reservationId",res.getReservationId(),"roomNumber",req.getRoomNumber(),"guestName",guest.getName(),"checkIn",req.getCheckIn().toString(),"checkOut",req.getCheckOut().toString(),"nights",nights,"totalAmount",fmt(totalPaise),"amenities",res.getAmenities(),"status",res.getStatus());
    }

    @Transactional
    public Map<String,Object> checkIn(String reservationId) {
        HotelReservation res = getReservation(reservationId);
        if (res.getStatus() != ReservationStatus.CONFIRMED) throw new HotelException("Reservation not in CONFIRMED state: " + res.getStatus(), HttpStatus.CONFLICT);
        if (LocalDate.now().isBefore(res.getCheckIn())) throw new HotelException("Check-in date is " + res.getCheckIn(), HttpStatus.BAD_REQUEST);
        res.setStatus(ReservationStatus.CHECKED_IN); resRepo.save(res);
        log.info("[HotelService] Checked in | reservationId={} room={} guest={}", reservationId, res.getRoom().getRoomNumber(), res.getGuest().getName());
        return Map.of("reservationId",reservationId,"status","CHECKED_IN","room",res.getRoom().getRoomNumber(),"message","Welcome! Enjoy your stay.");
    }

    @Transactional
    public Map<String,Object> checkOut(String reservationId) {
        HotelReservation res = getReservation(reservationId);
        if (res.getStatus() != ReservationStatus.CHECKED_IN) throw new HotelException("Guest not checked in: " + res.getStatus(), HttpStatus.CONFLICT);
        res.setStatus(ReservationStatus.CHECKED_OUT); resRepo.save(res);
        log.info("[HotelService] Checked out | reservationId={} total=Rs.{}", reservationId, res.getTotalPaise()/100.0);
        return Map.of("reservationId",reservationId,"status","CHECKED_OUT","totalBill",fmt(res.getTotalPaise()),"message","Thank you for staying with us!");
    }

    @Transactional
    public Map<String,Object> cancelReservation(String reservationId) {
        HotelReservation res = getReservation(reservationId);
        if (res.getStatus() == ReservationStatus.CHECKED_IN) throw new HotelException("Cannot cancel: guest is checked in", HttpStatus.CONFLICT);
        if (res.getStatus() == ReservationStatus.CHECKED_OUT || res.getStatus() == ReservationStatus.CANCELLED)
            throw new HotelException("Cannot cancel from status: " + res.getStatus(), HttpStatus.CONFLICT);
        res.setStatus(ReservationStatus.CANCELLED); resRepo.save(res);
        log.info("[HotelService] Reservation cancelled | id={}", reservationId);
        return Map.of("reservationId",reservationId,"status","CANCELLED","message","Reservation cancelled successfully");
    }

    private HotelReservation getReservation(String id) { return resRepo.findById(id).orElseThrow(() -> new HotelException("Reservation not found: "+id, HttpStatus.NOT_FOUND)); }
    private String fmt(long paise) { return String.format("Rs.%.2f", paise/100.0); }
}
