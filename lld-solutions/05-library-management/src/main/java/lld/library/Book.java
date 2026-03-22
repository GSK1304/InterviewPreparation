package lld.library;
import java.util.*;
import java.util.stream.Collectors;

public class Book {
    private final ISBN         isbn;
    private final String       title;
    private final List<Author> authors;
    private final String       genre;
    private final int          publishedYear;
    private final List<BookCopy> copies = new ArrayList<>();

    public Book(ISBN isbn, String title, List<Author> authors, String genre, int publishedYear) {
        this.isbn          = Objects.requireNonNull(isbn);
        this.title         = Objects.requireNonNull(title);
        this.authors       = List.copyOf(Objects.requireNonNull(authors));
        this.genre         = Objects.requireNonNull(genre);
        this.publishedYear = publishedYear;
        if (title.isBlank())    throw new IllegalArgumentException("Title cannot be blank");
        if (authors.isEmpty())  throw new IllegalArgumentException("Book must have at least one author");
    }

    public void addCopy(BookCopy copy) { copies.add(Objects.requireNonNull(copy)); }

    public Optional<BookCopy> findAvailableCopy() {
        return copies.stream().filter(BookCopy::isAvailable).findFirst();
    }

    public long availableCopiesCount() { return copies.stream().filter(BookCopy::isAvailable).count(); }

    public ISBN         getIsbn()    { return isbn; }
    public String       getTitle()   { return title; }
    public List<Author> getAuthors() { return authors; }
    public String       getGenre()   { return genre; }

    @Override public String toString() {
        return String.format("'%s' by %s (ISBN: %s) - %d/%d copies available",
            title,
            authors.stream().map(Author::fullName).collect(Collectors.joining(", ")),
            isbn, availableCopiesCount(), copies.size());
    }
}
