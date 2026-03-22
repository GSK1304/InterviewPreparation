package lld.atm;
public class WrongPINException extends ATMException {
    public final int attemptsRemaining;
    public WrongPINException(int remaining) {
        super("Wrong PIN. " + remaining + " attempt(s) remaining");
        this.attemptsRemaining = remaining;
    }
}
