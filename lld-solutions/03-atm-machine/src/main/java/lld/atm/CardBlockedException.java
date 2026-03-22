package lld.atm;
public class CardBlockedException extends ATMException {
    public CardBlockedException(String card) { super("Card blocked: " + card); }
}
