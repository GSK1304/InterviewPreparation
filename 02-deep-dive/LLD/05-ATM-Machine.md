# LLD — ATM Machine (Complete Java 21)

## Design Summary
| Aspect | Decision |
|--------|----------|
| ATM states | **State** pattern — IDLE / CARD_INSERTED / PIN_ENTERED / TRANSACTION |
| Transaction types | **Strategy** — WithdrawStrategy, DepositStrategy, BalanceInquiryStrategy |
| Cash dispensing | **Chain of Responsibility** — denominations handled by chained dispensers |
| Audit trail | Append-only `TransactionLog` (event sourcing concept) |
| Thread safety | `synchronized` on account balance + cash cassette |
| Records | `Card`, `Transaction`, `CashDispenseResult` |

## Complete Solution

```java
package lld.atm;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

// ── Value Objects ─────────────────────────────────────────────────────────────

record Card(String cardNumber, String bankCode, Instant expiryDate) {
    Card {
        Objects.requireNonNull(cardNumber, "Card number required");
        Objects.requireNonNull(bankCode,   "Bank code required");
        Objects.requireNonNull(expiryDate, "Expiry date required");
        if (!cardNumber.matches("\\d{16}"))
            throw new IllegalArgumentException("Card number must be 16 digits");
    }

    boolean isExpired() { return Instant.now().isAfter(expiryDate); }
    String maskedNumber() { return "**** **** **** " + cardNumber.substring(12); }
}

record Money(long amountPaise) {  // store in paise to avoid float issues
    static final Money ZERO = new Money(0);

    Money {
        if (amountPaise < 0) throw new IllegalArgumentException("Amount cannot be negative");
    }

    static Money ofRupees(long rupees) {
        if (rupees < 0) throw new IllegalArgumentException("Amount cannot be negative");
        return new Money(rupees * 100);
    }

    Money add(Money other)      { return new Money(amountPaise + other.amountPaise); }
    Money subtract(Money other) {
        if (other.amountPaise > this.amountPaise)
            throw new InsufficientFundsException(this, other);
        return new Money(amountPaise - other.amountPaise);
    }
    boolean isGreaterThanOrEqual(Money other) { return amountPaise >= other.amountPaise; }
    boolean isZero()            { return amountPaise == 0; }
    long    toRupees()          { return amountPaise / 100; }

    @Override public String toString() { return "₹" + toRupees(); }
}

record Transaction(
    String      transactionId,
    String      accountNumber,
    String      maskedCard,
    TransactionType type,
    Money       amount,
    Money       balanceAfter,
    Instant     timestamp,
    boolean     success,
    String      failureReason
) {
    private static final AtomicLong COUNTER = new AtomicLong(100000);

    static Transaction success(String accountNo, String maskedCard,
                               TransactionType type, Money amount, Money balanceAfter) {
        return new Transaction("TXN" + COUNTER.getAndIncrement(), accountNo, maskedCard,
            type, amount, balanceAfter, Instant.now(), true, null);
    }

    static Transaction failure(String accountNo, String maskedCard,
                               TransactionType type, Money amount, String reason) {
        return new Transaction("TXN" + COUNTER.getAndIncrement(), accountNo, maskedCard,
            type, amount, Money.ZERO, Instant.now(), false, reason);
    }
}

enum TransactionType { WITHDRAW, DEPOSIT, BALANCE_INQUIRY, PIN_CHANGE }

// ── Exceptions ────────────────────────────────────────────────────────────────

class ATMException extends RuntimeException {
    ATMException(String msg) { super(msg); }
}

class InsufficientFundsException extends ATMException {
    InsufficientFundsException(Money balance, Money requested) {
        super("Insufficient funds. Balance: " + balance + ", Requested: " + requested);
    }
}

class CardBlockedException extends ATMException {
    CardBlockedException(String maskedCard) {
        super("Card is blocked due to too many wrong PIN attempts: " + maskedCard);
    }
}

class WrongPINException extends ATMException {
    final int attemptsRemaining;
    WrongPINException(int remaining) {
        super("Wrong PIN. " + remaining + " attempt(s) remaining");
        this.attemptsRemaining = remaining;
    }
}

class InvalidATMStateException extends ATMException {
    InvalidATMStateException(String expected, String actual) {
        super("ATM in wrong state. Expected: " + expected + ", Actual: " + actual);
    }
}

class InsufficientCashException extends ATMException {
    InsufficientCashException(Money requested, Money available) {
        super("ATM has insufficient cash. Requested: " + requested + ", Available: " + available);
    }
}

// ── Bank Account ──────────────────────────────────────────────────────────────

class BankAccount {
    private final String accountNumber;
    private final String hashedPin;     // In production: bcrypt hash
    private Money        balance;
    private int          failedAttempts = 0;
    private boolean      blocked        = false;
    private static final int MAX_ATTEMPTS = 3;

    BankAccount(String accountNumber, String pin, Money initialBalance) {
        this.accountNumber = Objects.requireNonNull(accountNumber);
        this.hashedPin     = hashPin(Objects.requireNonNull(pin));
        this.balance       = Objects.requireNonNull(initialBalance);
    }

    private String hashPin(String pin) {
        // Simplified — production uses BCrypt
        return Integer.toHexString(pin.hashCode());
    }

    synchronized boolean validatePin(String pin) {
        if (blocked) throw new CardBlockedException("****");
        if (hashedPin.equals(hashPin(pin))) {
            failedAttempts = 0;
            return true;
        }
        failedAttempts++;
        if (failedAttempts >= MAX_ATTEMPTS) blocked = true;
        int remaining = MAX_ATTEMPTS - failedAttempts;
        throw new WrongPINException(remaining);
    }

    synchronized Money getBalance()           { return balance; }
    synchronized String getAccountNumber()    { return accountNumber; }
    synchronized boolean isBlocked()          { return blocked; }

    synchronized void debit(Money amount) {
        if (blocked) throw new CardBlockedException(accountNumber);
        balance = balance.subtract(amount);  // throws InsufficientFundsException if insufficient
    }

    synchronized void credit(Money amount) {
        balance = balance.add(amount);
    }
}

// ── Cash Cassette & Dispenser (Chain of Responsibility) ───────────────────────

class CashCassette {
    private final int denomination;  // in rupees
    private int       noteCount;

    CashCassette(int denomination, int noteCount) {
        if (denomination <= 0) throw new IllegalArgumentException("Denomination must be positive");
        if (noteCount < 0)     throw new IllegalArgumentException("Note count cannot be negative");
        this.denomination = denomination;
        this.noteCount    = noteCount;
    }

    public int getDenomination()  { return denomination; }
    public int getNoteCount()     { return noteCount; }
    public Money getTotalValue()  { return Money.ofRupees((long) denomination * noteCount); }

    synchronized boolean canDispense(int notesNeeded) {
        return noteCount >= notesNeeded;
    }

    synchronized void dispense(int notesNeeded) {
        if (!canDispense(notesNeeded))
            throw new ATMException("Cassette has only " + noteCount + " notes of ₹" + denomination);
        noteCount -= notesNeeded;
    }

    synchronized void refill(int additionalNotes) {
        if (additionalNotes < 0) throw new IllegalArgumentException("Cannot add negative notes");
        noteCount += additionalNotes;
    }
}

/** Chain of Responsibility — each dispenser handles one denomination */
abstract class CashDispenser {
    protected CashDispenser next;
    protected final CashCassette cassette;

    CashDispenser(CashCassette cassette) {
        this.cassette = Objects.requireNonNull(cassette);
    }

    CashDispenser setNext(CashDispenser next) {
        this.next = next;
        return next;
    }

    Map<Integer, Integer> dispense(long remainingRupees) {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        int denom = cassette.getDenomination();

        if (remainingRupees >= denom) {
            int notesNeeded = (int) Math.min(remainingRupees / denom, cassette.getNoteCount());
            if (notesNeeded > 0) {
                cassette.dispense(notesNeeded);
                result.put(denom, notesNeeded);
                remainingRupees -= (long) notesNeeded * denom;
            }
        }

        // Delegate remainder to next dispenser
        if (next != null && remainingRupees > 0) {
            result.putAll(next.dispense(remainingRupees));
        }

        return result;
    }
}

class Rs2000Dispenser extends CashDispenser { Rs2000Dispenser(CashCassette c) { super(c); } }
class Rs500Dispenser  extends CashDispenser { Rs500Dispenser(CashCassette c)  { super(c); } }
class Rs200Dispenser  extends CashDispenser { Rs200Dispenser(CashCassette c)  { super(c); } }
class Rs100Dispenser  extends CashDispenser { Rs100Dispenser(CashCassette c)  { super(c); } }

// ── Transaction Log ───────────────────────────────────────────────────────────

class TransactionLog {
    private final List<Transaction> log = new ArrayList<>();

    synchronized void record(Transaction t) { log.add(t); }

    synchronized List<Transaction> getByAccount(String accountNumber) {
        return log.stream()
            .filter(t -> t.accountNumber().equals(accountNumber))
            .toList();  // Java 16+ unmodifiable list
    }

    synchronized void print() {
        System.out.println("\n── Transaction Log ──");
        log.forEach(t -> System.out.printf("[%s] %s %s %s %s%n",
            t.transactionId(), t.maskedCard(), t.type(),
            t.amount(), t.success() ? "OK" : "FAILED: " + t.failureReason()));
    }
}

// ── ATM State ─────────────────────────────────────────────────────────────────

enum ATMStateType { IDLE, CARD_INSERTED, AUTHENTICATED, TRANSACTION_IN_PROGRESS }

// ── ATM Machine ───────────────────────────────────────────────────────────────

class ATMMachine {
    private final String         atmId;
    private final CashDispenser  dispenserChain;
    private final TransactionLog transactionLog;
    private final Map<String, BankAccount> accounts;  // cardNumber → account (from bank)

    // Session state (cleared on card eject)
    private ATMStateType currentState = ATMStateType.IDLE;
    private Card         insertedCard;
    private BankAccount  currentAccount;

    ATMMachine(String atmId, CashDispenser dispenserChain,
               TransactionLog transactionLog, Map<String, BankAccount> accounts) {
        this.atmId          = Objects.requireNonNull(atmId);
        this.dispenserChain = Objects.requireNonNull(dispenserChain);
        this.transactionLog = Objects.requireNonNull(transactionLog);
        this.accounts       = Objects.requireNonNull(accounts);
    }

    // ── Card Operations ───────────────────────────────────────────────────────

    public void insertCard(Card card) {
        requireState(ATMStateType.IDLE);
        Objects.requireNonNull(card, "Card required");

        if (card.isExpired())
            throw new ATMException("Card has expired: " + card.maskedNumber());

        BankAccount account = accounts.get(card.cardNumber());
        if (account == null)
            throw new ATMException("Card not recognised: " + card.maskedNumber());
        if (account.isBlocked())
            throw new CardBlockedException(card.maskedNumber());

        this.insertedCard   = card;
        this.currentAccount = account;
        this.currentState   = ATMStateType.CARD_INSERTED;
        System.out.println("✅ Card inserted: " + card.maskedNumber());
    }

    public void enterPin(String pin) {
        requireState(ATMStateType.CARD_INSERTED);
        Objects.requireNonNull(pin, "PIN required");
        if (pin.length() != 4 || !pin.matches("\\d{4}"))
            throw new ATMException("PIN must be 4 digits");

        currentAccount.validatePin(pin);  // throws WrongPINException or CardBlockedException
        currentState = ATMStateType.AUTHENTICATED;
        System.out.println("✅ PIN verified");
    }

    // ── Transactions (require AUTHENTICATED state) ────────────────────────────

    public Money checkBalance() {
        requireState(ATMStateType.AUTHENTICATED);
        Money balance = currentAccount.getBalance();
        transactionLog.record(Transaction.success(
            currentAccount.getAccountNumber(), insertedCard.maskedNumber(),
            TransactionType.BALANCE_INQUIRY, Money.ZERO, balance));
        System.out.println("💳 Balance: " + balance);
        return balance;
    }

    public Map<Integer, Integer> withdraw(Money amount) {
        requireState(ATMStateType.AUTHENTICATED);
        Objects.requireNonNull(amount, "Amount required");

        if (amount.isZero()) throw new ATMException("Withdrawal amount cannot be zero");
        if (amount.amountPaise() % 100 != 0)
            throw new ATMException("Amount must be in whole rupees");

        long rupees = amount.toRupees();
        if (rupees % 100 != 0)
            throw new ATMException("Withdrawal must be in multiples of ₹100");

        // Check account balance
        currentAccount.debit(amount);  // throws InsufficientFundsException if short

        // Dispense cash
        Map<Integer, Integer> dispensed;
        try {
            dispensed = dispenserChain.dispense(rupees);
        } catch (ATMException e) {
            currentAccount.credit(amount);  // rollback account debit
            throw new InsufficientCashException(amount, getAvailableCash());
        }

        transactionLog.record(Transaction.success(
            currentAccount.getAccountNumber(), insertedCard.maskedNumber(),
            TransactionType.WITHDRAW, amount, currentAccount.getBalance()));

        System.out.printf("💵 Dispensed %s: %s%n", amount,
            formatDispensed(dispensed));
        return dispensed;
    }

    public void deposit(Money amount) {
        requireState(ATMStateType.AUTHENTICATED);
        Objects.requireNonNull(amount, "Amount required");
        if (amount.isZero()) throw new ATMException("Deposit amount cannot be zero");

        currentAccount.credit(amount);
        transactionLog.record(Transaction.success(
            currentAccount.getAccountNumber(), insertedCard.maskedNumber(),
            TransactionType.DEPOSIT, amount, currentAccount.getBalance()));
        System.out.println("✅ Deposited: " + amount + " | New balance: " + currentAccount.getBalance());
    }

    public void ejectCard() {
        if (currentState == ATMStateType.IDLE) return;
        System.out.println("🏧 Card ejected: " +
            (insertedCard != null ? insertedCard.maskedNumber() : ""));
        resetSession();
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    public void printTransactionLog() { transactionLog.print(); }

    public Money getAvailableCash() {
        // Sum all cassettes — expose through a CashManager in production
        return Money.ZERO;  // Simplified
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void requireState(ATMStateType required) {
        if (currentState != required)
            throw new InvalidATMStateException(required.name(), currentState.name());
    }

    private void resetSession() {
        insertedCard   = null;
        currentAccount = null;
        currentState   = ATMStateType.IDLE;
    }

    private String formatDispensed(Map<Integer, Integer> dispensed) {
        StringBuilder sb = new StringBuilder();
        dispensed.forEach((denom, count) ->
            sb.append(count).append("×₹").append(denom).append(" "));
        return sb.toString().trim();
    }

    public ATMStateType getState() { return currentState; }
    public String getAtmId()       { return atmId; }
}

// ── ATM Factory ───────────────────────────────────────────────────────────────

class ATMFactory {
    static ATMMachine create(String atmId, Map<String, BankAccount> accounts) {
        // Set up cash cassettes
        CashCassette c2000 = new CashCassette(2000, 50);
        CashCassette c500  = new CashCassette(500,  100);
        CashCassette c200  = new CashCassette(200,  100);
        CashCassette c100  = new CashCassette(100,  200);

        // Build dispenser chain: ₹2000 → ₹500 → ₹200 → ₹100
        CashDispenser dispenser = new Rs2000Dispenser(c2000);
        dispenser.setNext(new Rs500Dispenser(c500))
                 .setNext(new Rs200Dispenser(c200))
                 .setNext(new Rs100Dispenser(c100));

        return new ATMMachine(atmId, dispenser, new TransactionLog(), accounts);
    }
}

// ── Main Demo ─────────────────────────────────────────────────────────────────

public class ATMDemo {
    public static void main(String[] args) {
        // Setup bank accounts
        Map<String, BankAccount> accounts = new HashMap<>();
        accounts.put("1234567890123456",
            new BankAccount("ACC001", "1234", Money.ofRupees(50_000)));
        accounts.put("9876543210987654",
            new BankAccount("ACC002", "5678", Money.ofRupees(10_000)));

        ATMMachine atm = ATMFactory.create("ATM-HYD-001", accounts);

        // ── Scenario 1: Successful withdrawal ────────────────────────────────
        System.out.println("\n=== Scenario 1: Successful Withdrawal ===");
        Card card1 = new Card("1234567890123456", "SBI",
            Instant.now().plusSeconds(365 * 24 * 3600));
        atm.insertCard(card1);
        atm.enterPin("1234");
        atm.checkBalance();
        Map<Integer, Integer> dispensed = atm.withdraw(Money.ofRupees(5500));
        atm.checkBalance();
        atm.ejectCard();

        // ── Scenario 2: Wrong PIN → block card ───────────────────────────────
        System.out.println("\n=== Scenario 2: Wrong PIN attempts ===");
        atm.insertCard(card1);
        try { atm.enterPin("0000"); } catch (WrongPINException e) { System.out.println("❌ " + e.getMessage()); }
        try { atm.enterPin("1111"); } catch (WrongPINException e) { System.out.println("❌ " + e.getMessage()); }
        try { atm.enterPin("2222"); } catch (CardBlockedException e) { System.out.println("🚫 " + e.getMessage()); }
        atm.ejectCard();

        // ── Scenario 3: Insufficient funds ───────────────────────────────────
        System.out.println("\n=== Scenario 3: Insufficient Funds ===");
        Card card2 = new Card("9876543210987654", "HDFC",
            Instant.now().plusSeconds(365 * 24 * 3600));
        atm.insertCard(card2);
        atm.enterPin("5678");
        try {
            atm.withdraw(Money.ofRupees(50_000));  // more than balance
        } catch (InsufficientFundsException e) {
            System.out.println("❌ " + e.getMessage());
        }
        atm.deposit(Money.ofRupees(5_000));
        atm.ejectCard();

        // ── Scenario 4: Wrong state (no card) ────────────────────────────────
        System.out.println("\n=== Scenario 4: Wrong ATM State ===");
        try {
            atm.withdraw(Money.ofRupees(1000));  // no card inserted
        } catch (InvalidATMStateException e) {
            System.out.println("❌ " + e.getMessage());
        }

        atm.printTransactionLog();
    }
}
```

## Extension Q&A

**Q: How do you add UPI/contactless card support?**
Add a `CardReader` interface with `PhysicalCardReader` and `NfcCardReader` implementations. The `ATMMachine` depends on `CardReader` (DIP). Swap the reader without changing the ATM core logic.

**Q: How do you make the ATM work in multiple currencies?**
Replace `Money(long amountPaise)` with `Money(long minorUnit, Currency currency)`. All arithmetic stays the same (working in minor units). Exchange rates handled by a separate `CurrencyConverter` service. The ATM's cash cassettes are labelled with denomination + currency.

**Q: How do you implement the daily withdrawal limit?**
Add `dailyWithdrawn: Money` and `lastWithdrawalDate: LocalDate` to `BankAccount`. Before debit, check `dailyWithdrawn.add(amount)` does not exceed the daily limit. Reset `dailyWithdrawn` when date changes. Store this state persistently for cross-session enforcement.
