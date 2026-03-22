package lld.library;
public class BorrowLimitExceededException extends LibraryException {
    public BorrowLimitExceededException(String id, int limit) {
        super("Member " + id + " reached borrow limit of " + limit);
    }
}
