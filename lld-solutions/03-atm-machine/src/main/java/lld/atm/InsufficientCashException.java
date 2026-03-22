package lld.atm;
public class InsufficientCashException extends ATMException {
    public InsufficientCashException(Money r, Money a) {
        super("ATM insufficient cash. Requested: " + r + ", Available: " + a);
    }
}
