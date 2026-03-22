package lld.bookmyshow;
import java.time.*;
import java.util.Objects;

public class Seat {
    private final String   seatId;
    private final int      row, col;
    private final SeatTier tier;
    private SeatStatus     status = SeatStatus.AVAILABLE;
    private String         lockedByBookingId;
    private Instant        lockExpiry;

    public Seat(String seatId, int row, int col, SeatTier tier) {
        this.seatId = Objects.requireNonNull(seatId);
        this.row = row; this.col = col;
        this.tier = Objects.requireNonNull(tier);
    }

    public synchronized boolean tryLock(String bookingId, Duration ttl) {
        releaseLockIfExpired();
        if (status != SeatStatus.AVAILABLE) return false;
        status = SeatStatus.LOCKED; lockedByBookingId = bookingId;
        lockExpiry = Instant.now().plus(ttl);
        return true;
    }

    public synchronized boolean confirm(String bookingId) {
        if (status == SeatStatus.LOCKED && bookingId.equals(lockedByBookingId)) {
            status = SeatStatus.BOOKED; return true;
        }
        return false;
    }

    public synchronized void release(String bookingId) {
        if (bookingId.equals(lockedByBookingId)) {
            status = SeatStatus.AVAILABLE; lockedByBookingId = null; lockExpiry = null;
        }
    }

    public synchronized void releaseLockIfExpired() {
        if (status == SeatStatus.LOCKED && lockExpiry != null && Instant.now().isAfter(lockExpiry)) {
            status = SeatStatus.AVAILABLE; lockedByBookingId = null; lockExpiry = null;
        }
    }

    public synchronized boolean isAvailable() { releaseLockIfExpired(); return status == SeatStatus.AVAILABLE; }

    public String    getSeatId() { return seatId; }
    public int       getRow()    { return row; }
    public int       getCol()    { return col; }
    public SeatTier  getTier()   { return tier; }
    public SeatStatus getStatus(){ return status; }
    @Override public String toString() { return String.format("[%s R%dC%d %s %s]", seatId, row, col, tier, status); }
}
