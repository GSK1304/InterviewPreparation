package lld.library;
public class BookNotFoundException extends LibraryException {
    public BookNotFoundException(String id) { super("Book not found: " + id); }
}
