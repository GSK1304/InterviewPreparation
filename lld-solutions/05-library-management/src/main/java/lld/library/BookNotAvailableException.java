package lld.library;
public class BookNotAvailableException extends LibraryException {
    public BookNotAvailableException(String isbn) { super("No available copy for ISBN: " + isbn); }
}
