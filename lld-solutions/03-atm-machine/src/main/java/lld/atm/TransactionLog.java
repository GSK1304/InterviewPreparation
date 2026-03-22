package lld.atm;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
public class TransactionLog {
    private record Entry(long id, String account, String maskedCard,
                         TransactionType type, Money amount, boolean success, Instant at) {}
    private final List<Entry>    log     = new ArrayList<>();
    private final AtomicLong     counter = new AtomicLong(100000);

    public synchronized void record(String account, String maskedCard,
                                    TransactionType type, Money amount, boolean success) {
        log.add(new Entry(counter.getAndIncrement(), account, maskedCard, type, amount, success, Instant.now()));
    }

    public synchronized void print() {
        System.out.println("\n--- Transaction Log ---");
        log.forEach(e -> System.out.printf("[TXN-%d] %s %s %s %s %s%n",
            e.id(), e.maskedCard(), e.type(), e.amount(), e.success() ? "OK" : "FAILED", e.at()));
    }
}
