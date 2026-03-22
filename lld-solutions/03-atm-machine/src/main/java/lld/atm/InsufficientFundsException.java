package lld.atm;
public class InsufficientFundsException extends ATMException {
    public InsufficientFundsException(Money balance, Money requested) {
        super("Insufficient funds. Balance: " + balance + ", Requested: " + requested);
    }
}
