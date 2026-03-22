package lld.atm;
import java.util.*;

public class ATMMachine {
    private final String           atmId;
    private final CashDispenser    dispenserChain;
    private final TransactionLog   txLog;
    private final Map<String, BankAccount> accounts;

    private ATMStateType currentState = ATMStateType.IDLE;
    private Card         insertedCard;
    private BankAccount  currentAccount;

    public ATMMachine(String atmId, CashDispenser dispenserChain,
                      TransactionLog txLog, Map<String, BankAccount> accounts) {
        this.atmId         = Objects.requireNonNull(atmId);
        this.dispenserChain= Objects.requireNonNull(dispenserChain);
        this.txLog         = Objects.requireNonNull(txLog);
        this.accounts      = Objects.requireNonNull(accounts);
    }

    public void insertCard(Card card) {
        requireState(ATMStateType.IDLE);
        Objects.requireNonNull(card);
        if (card.isExpired()) throw new ATMException("Card expired: " + card.maskedNumber());
        BankAccount account = accounts.get(card.cardNumber());
        if (account == null) throw new ATMException("Card not recognised: " + card.maskedNumber());
        if (account.isBlocked()) throw new CardBlockedException(card.maskedNumber());
        this.insertedCard   = card;
        this.currentAccount = account;
        this.currentState   = ATMStateType.CARD_INSERTED;
        System.out.println("Card inserted: " + card.maskedNumber());
    }

    public void enterPin(String pin) {
        requireState(ATMStateType.CARD_INSERTED);
        if (pin == null || !pin.matches("\\d{4}")) throw new ATMException("PIN must be 4 digits");
        currentAccount.validatePin(pin);
        currentState = ATMStateType.AUTHENTICATED;
        System.out.println("PIN verified");
    }

    public Money checkBalance() {
        requireState(ATMStateType.AUTHENTICATED);
        Money balance = currentAccount.getBalance();
        txLog.record(currentAccount.getAccountNumber(), insertedCard.maskedNumber(),
            TransactionType.BALANCE_INQUIRY, Money.ZERO, true);
        System.out.println("Balance: " + balance);
        return balance;
    }

    public Map<Integer, Integer> withdraw(Money amount) {
        requireState(ATMStateType.AUTHENTICATED);
        Objects.requireNonNull(amount);
        if (amount.isZero()) throw new ATMException("Amount cannot be zero");
        if (amount.toRupees() % 100 != 0) throw new ATMException("Must be in multiples of Rs.100");

        currentAccount.debit(amount);
        Map<Integer, Integer> dispensed;
        try {
            dispensed = dispenserChain.dispense(amount.toRupees());
        } catch (ATMException e) {
            currentAccount.credit(amount);
            throw new InsufficientCashException(amount, Money.ZERO);
        }

        txLog.record(currentAccount.getAccountNumber(), insertedCard.maskedNumber(),
            TransactionType.WITHDRAW, amount, true);
        System.out.printf("Dispensed %s: %s%n", amount, formatNotes(dispensed));
        return dispensed;
    }

    public void deposit(Money amount) {
        requireState(ATMStateType.AUTHENTICATED);
        Objects.requireNonNull(amount);
        if (amount.isZero()) throw new ATMException("Amount cannot be zero");
        currentAccount.credit(amount);
        txLog.record(currentAccount.getAccountNumber(), insertedCard.maskedNumber(),
            TransactionType.DEPOSIT, amount, true);
        System.out.println("Deposited: " + amount + " | New balance: " + currentAccount.getBalance());
    }

    public void ejectCard() {
        if (currentState != ATMStateType.IDLE)
            System.out.println("Card ejected: " + (insertedCard != null ? insertedCard.maskedNumber() : ""));
        resetSession();
    }

    public void printLog() { txLog.print(); }

    private void requireState(ATMStateType required) {
        if (currentState != required)
            throw new InvalidATMStateException(required.name(), currentState.name());
    }

    private void resetSession() {
        insertedCard = null; currentAccount = null; currentState = ATMStateType.IDLE;
    }

    private String formatNotes(Map<Integer, Integer> notes) {
        StringBuilder sb = new StringBuilder();
        notes.forEach((d, n) -> sb.append(n).append("xRs.").append(d).append(" "));
        return sb.toString().trim();
    }
}
