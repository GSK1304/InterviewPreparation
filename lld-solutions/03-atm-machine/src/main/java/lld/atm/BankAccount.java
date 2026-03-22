package lld.atm;
import java.util.Objects;
public class BankAccount {
    private final String accountNumber;
    private final String hashedPin;
    private Money        balance;
    private int          failedAttempts = 0;
    private boolean      blocked        = false;
    private static final int MAX_ATTEMPTS = 3;

    public BankAccount(String accountNumber, String pin, Money initialBalance) {
        this.accountNumber = Objects.requireNonNull(accountNumber);
        this.hashedPin     = hashPin(Objects.requireNonNull(pin));
        this.balance       = Objects.requireNonNull(initialBalance);
    }

    private String hashPin(String pin) { return Integer.toHexString(pin.hashCode()); }

    public synchronized boolean validatePin(String pin) {
        if (blocked) throw new CardBlockedException(accountNumber);
        if (hashedPin.equals(hashPin(pin))) { failedAttempts = 0; return true; }
        failedAttempts++;
        if (failedAttempts >= MAX_ATTEMPTS) blocked = true;
        throw new WrongPINException(MAX_ATTEMPTS - failedAttempts);
    }

    public synchronized Money  getBalance()        { return balance; }
    public synchronized String getAccountNumber()  { return accountNumber; }
    public synchronized boolean isBlocked()        { return blocked; }

    public synchronized void debit(Money amount) {
        if (blocked) throw new CardBlockedException(accountNumber);
        balance = balance.subtract(amount);
    }
    public synchronized void credit(Money amount)  { balance = balance.add(amount); }
}
