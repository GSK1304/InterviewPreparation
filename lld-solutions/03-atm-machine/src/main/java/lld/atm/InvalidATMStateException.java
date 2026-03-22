package lld.atm;
public class InvalidATMStateException extends ATMException {
    public InvalidATMStateException(String expected, String actual) {
        super("ATM state error. Expected: " + expected + ", Actual: " + actual);
    }
}
