package lld.library;
public class OutstandingFineException extends LibraryException {
    public OutstandingFineException(String id, double fine) {
        super("Member " + id + " has outstanding fine: Rs." + fine);
    }
}
