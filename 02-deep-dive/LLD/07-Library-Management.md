# LLD — Library Management System (Complete Java 21)

## Design Summary
| Aspect | Decision |
|--------|----------|
| Book copies | `Book` (catalog entry) vs `BookCopy` (physical copy with barcode) |
| Borrowing | `Loan` record tracks who borrowed which copy and when |
| Reservation | `Reservation` queue per book — FIFO, notified when copy returned |
| Fine calculation | Strategy pattern — `FineCalculator` interface |
| Search | Separate `BookCatalog` handles search across multiple fields |
| Member tiers | `MemberType` enum drives borrow limits and loan duration |

## Complete Solution

```java
package lld.library;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

// ── Enums ─────────────────────────────────────────────────────────────────────

enum BookStatus  { AVAILABLE, BORROWED, RESERVED, LOST, DAMAGED }
enum MemberType  { STUDENT, FACULTY, PUBLIC }
enum LoanStatus  { ACTIVE, RETURNED, OVERDUE }

// ── Value Objects ─────────────────────────────────────────────────────────────

record ISBN(String value) {
    ISBN {
        Objects.requireNonNull(value, "ISBN required");
        String digits = value.replaceAll("[-\\s]", "");
        if (digits.length() != 10 && digits.length() != 13)
            throw new IllegalArgumentException("ISBN must be 10 or 13 digits: " + value);
    }
    @Override public String toString() { return value; }
}

record Author(String firstName, String lastName) {
    Author {
        Objects.requireNonNull(firstName, "First name required");
        Objects.requireNonNull(lastName,  "Last name required");
        if (firstName.isBlank()) throw new IllegalArgumentException("First name cannot be blank");
        if (lastName.isBlank())  throw new IllegalArgumentException("Last name cannot be blank");
    }
    public String fullName() { return firstName + " " + lastName; }
}

record Fine(double amountRupees, int overdueDays) {
    Fine {
        if (amountRupees < 0) throw new IllegalArgumentException("Fine cannot be negative");
        if (overdueDays  < 0) throw new IllegalArgumentException("Overdue days cannot be negative");
    }
    static final Fine NONE = new Fine(0, 0);
    boolean isOwed() { return amountRupees > 0; }
    @Override public String toString() {
        return isOwed() ? String.format("₹%.2f (%d days overdue)", amountRupees, overdueDays) : "No fine";
    }
}

// ── Exceptions ────────────────────────────────────────────────────────────────

class LibraryException extends RuntimeException {
    LibraryException(String msg) { super(msg); }
}

class BookNotFoundException extends LibraryException {
    BookNotFoundException(String id) { super("Book not found: " + id); }
}

class MemberNotFoundException extends LibraryException {
    MemberNotFoundException(String id) { super("Member not found: " + id); }
}

class BorrowLimitExceededException extends LibraryException {
    BorrowLimitExceededException(String memberId, int limit) {
        super("Member " + memberId + " has reached borrow limit of " + limit);
    }
}

class BookNotAvailableException extends LibraryException {
    BookNotAvailableException(String isbn) {
        super("No available copy for ISBN: " + isbn);
    }
}

class OutstandingFineException extends LibraryException {
    OutstandingFineException(String memberId, Fine fine) {
        super("Member " + memberId + " has outstanding fine: " + fine);
    }
}

// ── Fine Calculator (Strategy) ───────────────────────────────────────────────

interface FineCalculator {
    Fine calculate(LocalDate dueDate, LocalDate returnDate);
}

class StandardFineCalculator implements FineCalculator {
    private static final double FINE_PER_DAY = 2.0; // ₹2 per day

    @Override
    public Fine calculate(LocalDate dueDate, LocalDate returnDate) {
        if (!returnDate.isAfter(dueDate)) return Fine.NONE;
        int days  = (int) ChronoUnit.DAYS.between(dueDate, returnDate);
        double amount = days * FINE_PER_DAY;
        return new Fine(amount, days);
    }
}

// ── Book ──────────────────────────────────────────────────────────────────────

class Book {
    private final ISBN         isbn;
    private final String       title;
    private final List<Author> authors;
    private final String       genre;
    private final int          publishedYear;
    private final List<BookCopy> copies = new ArrayList<>();

    Book(ISBN isbn, String title, List<Author> authors, String genre, int publishedYear) {
        this.isbn          = Objects.requireNonNull(isbn);
        this.title         = Objects.requireNonNull(title);
        this.authors       = List.copyOf(Objects.requireNonNull(authors));
        this.genre         = Objects.requireNonNull(genre);
        this.publishedYear = publishedYear;
        if (title.isBlank())   throw new IllegalArgumentException("Title cannot be blank");
        if (authors.isEmpty()) throw new IllegalArgumentException("Book must have at least one author");
        if (publishedYear < 1000 || publishedYear > Year.now().getValue())
            throw new IllegalArgumentException("Invalid year: " + publishedYear);
    }

    void addCopy(BookCopy copy) { copies.add(Objects.requireNonNull(copy)); }

    Optional<BookCopy> findAvailableCopy() {
        return copies.stream().filter(c -> c.getStatus() == BookStatus.AVAILABLE).findFirst();
    }

    long availableCopiesCount() {
        return copies.stream().filter(c -> c.getStatus() == BookStatus.AVAILABLE).count();
    }

    public ISBN         getIsbn()          { return isbn; }
    public String       getTitle()         { return title; }
    public List<Author> getAuthors()       { return authors; }
    public String       getGenre()         { return genre; }
    public List<BookCopy> getCopies()      { return Collections.unmodifiableList(copies); }

    @Override public String toString() {
        return String.format("'%s' by %s (ISBN: %s) — %d/%d copies available",
            title,
            authors.stream().map(Author::fullName).collect(Collectors.joining(", ")),
            isbn,
            availableCopiesCount(),
            copies.size());
    }
}

// ── Book Copy ─────────────────────────────────────────────────────────────────

class BookCopy {
    private final String     barcode;
    private final ISBN       isbn;
    private BookStatus       status;

    BookCopy(String barcode, ISBN isbn) {
        this.barcode = Objects.requireNonNull(barcode);
        this.isbn    = Objects.requireNonNull(isbn);
        this.status  = BookStatus.AVAILABLE;
        if (barcode.isBlank()) throw new IllegalArgumentException("Barcode cannot be blank");
    }

    public String     getBarcode() { return barcode; }
    public ISBN       getIsbn()    { return isbn; }
    public BookStatus getStatus()  { return status; }
    void setStatus(BookStatus s)   { this.status = Objects.requireNonNull(s); }
}

// ── Member ────────────────────────────────────────────────────────────────────

class Member {
    private final String     memberId;
    private final String     name;
    private final String     email;
    private final MemberType type;
    private final int        borrowLimit;
    private final int        loanDurationDays;
    private double           outstandingFine = 0.0;

    Member(String memberId, String name, String email, MemberType type) {
        this.memberId         = Objects.requireNonNull(memberId);
        this.name             = Objects.requireNonNull(name);
        this.email            = Objects.requireNonNull(email);
        this.type             = Objects.requireNonNull(type);
        this.borrowLimit      = switch (type) {
            case STUDENT  -> 3;
            case FACULTY  -> 10;
            case PUBLIC   -> 2;
        };
        this.loanDurationDays = switch (type) {
            case STUDENT  -> 14;
            case FACULTY  -> 30;
            case PUBLIC   -> 7;
        };
        if (name.isBlank())   throw new IllegalArgumentException("Name cannot be blank");
        if (!email.contains("@")) throw new IllegalArgumentException("Invalid email: " + email);
    }

    public String     getMemberId()        { return memberId; }
    public String     getName()            { return name; }
    public String     getEmail()           { return email; }
    public MemberType getType()            { return type; }
    public int        getBorrowLimit()     { return borrowLimit; }
    public int        getLoanDurationDays(){ return loanDurationDays; }
    public double     getOutstandingFine() { return outstandingFine; }

    void addFine(double amount) {
        if (amount < 0) throw new IllegalArgumentException("Fine cannot be negative");
        outstandingFine += amount;
    }

    void payFine(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Payment must be positive");
        if (amount > outstandingFine)
            throw new LibraryException("Payment exceeds outstanding fine");
        outstandingFine -= amount;
    }

    boolean hasFine()    { return outstandingFine > 0; }

    @Override public String toString() {
        return String.format("Member[%s %s (%s) fine=₹%.2f]",
            memberId, name, type, outstandingFine);
    }
}

// ── Loan ──────────────────────────────────────────────────────────────────────

class Loan {
    private final String    loanId;
    private final Member    member;
    private final BookCopy  copy;
    private final LocalDate borrowDate;
    private final LocalDate dueDate;
    private LocalDate       returnDate;
    private LoanStatus      status;

    private static int counter = 1000;

    Loan(Member member, BookCopy copy, int loanDurationDays) {
        this.loanId     = "LN" + counter++;
        this.member     = Objects.requireNonNull(member);
        this.copy       = Objects.requireNonNull(copy);
        this.borrowDate = LocalDate.now();
        this.dueDate    = borrowDate.plusDays(loanDurationDays);
        this.status     = LoanStatus.ACTIVE;
    }

    void returnBook(LocalDate returnDate) {
        Objects.requireNonNull(returnDate, "Return date required");
        if (returnDate.isBefore(borrowDate))
            throw new IllegalArgumentException("Return date cannot be before borrow date");
        this.returnDate = returnDate;
        this.status     = returnDate.isAfter(dueDate) ? LoanStatus.OVERDUE : LoanStatus.RETURNED;
    }

    boolean isOverdue() { return LocalDate.now().isAfter(dueDate) && status == LoanStatus.ACTIVE; }

    public String    getLoanId()    { return loanId; }
    public Member    getMember()    { return member; }
    public BookCopy  getCopy()      { return copy; }
    public LocalDate getBorrowDate(){ return borrowDate; }
    public LocalDate getDueDate()   { return dueDate; }
    public LocalDate getReturnDate(){ return returnDate; }
    public LoanStatus getStatus()  { return status; }

    @Override public String toString() {
        return String.format("Loan[%s %s borrows '%s' due=%s status=%s]",
            loanId, member.getName(), copy.getIsbn(), dueDate, status);
    }
}

// ── Reservation ───────────────────────────────────────────────────────────────

class Reservation {
    private final String    reservationId;
    private final Member    member;
    private final ISBN      isbn;
    private final LocalDate reservedDate;
    private boolean         notified = false;

    private static int counter = 5000;

    Reservation(Member member, ISBN isbn) {
        this.reservationId = "RES" + counter++;
        this.member        = Objects.requireNonNull(member);
        this.isbn          = Objects.requireNonNull(isbn);
        this.reservedDate  = LocalDate.now();
    }

    void notify(String message) {
        System.out.printf("📧 Email to %s: %s%n", member.getEmail(), message);
        notified = true;
    }

    public Member getMember()          { return member; }
    public ISBN   getIsbn()            { return isbn; }
    public String getReservationId()   { return reservationId; }
    public boolean isNotified()        { return notified; }
}

// ── Book Catalog ──────────────────────────────────────────────────────────────

class BookCatalog {
    private final Map<ISBN, Book>   booksByIsbn  = new LinkedHashMap<>();
    private final Map<String, Loan> activeLoans  = new HashMap<>();
    // barcode → loan
    private final Map<ISBN, Queue<Reservation>> reservations = new HashMap<>();

    void addBook(Book book) {
        Objects.requireNonNull(book, "Book required");
        booksByIsbn.put(book.getIsbn(), book);
    }

    void addCopy(ISBN isbn, BookCopy copy) {
        Book book = getBook(isbn);
        book.addCopy(copy);
    }

    Book getBook(ISBN isbn) {
        Book book = booksByIsbn.get(Objects.requireNonNull(isbn));
        if (book == null) throw new BookNotFoundException(isbn.value());
        return book;
    }

    List<Book> searchByTitle(String title) {
        String lower = title.toLowerCase();
        return booksByIsbn.values().stream()
            .filter(b -> b.getTitle().toLowerCase().contains(lower))
            .collect(Collectors.toList());
    }

    List<Book> searchByAuthor(String authorName) {
        String lower = authorName.toLowerCase();
        return booksByIsbn.values().stream()
            .filter(b -> b.getAuthors().stream()
                .anyMatch(a -> a.fullName().toLowerCase().contains(lower)))
            .collect(Collectors.toList());
    }

    List<Book> searchByGenre(String genre) {
        return booksByIsbn.values().stream()
            .filter(b -> b.getGenre().equalsIgnoreCase(genre))
            .collect(Collectors.toList());
    }

    Optional<Loan> getActiveLoan(String barcode) {
        return Optional.ofNullable(activeLoans.get(barcode));
    }

    void recordLoan(Loan loan) {
        activeLoans.put(loan.getCopy().getBarcode(), loan);
    }

    void removeLoan(String barcode) {
        activeLoans.remove(barcode);
    }

    void addReservation(Reservation res) {
        reservations.computeIfAbsent(res.getIsbn(), k -> new LinkedList<>()).add(res);
    }

    Optional<Reservation> nextReservation(ISBN isbn) {
        Queue<Reservation> queue = reservations.get(isbn);
        return (queue != null && !queue.isEmpty()) ? Optional.of(queue.poll()) : Optional.empty();
    }

    boolean hasReservation(ISBN isbn, String memberId) {
        Queue<Reservation> queue = reservations.get(isbn);
        return queue != null && queue.stream()
            .anyMatch(r -> r.getMember().getMemberId().equals(memberId));
    }

    Map<String, Loan> getActiveLoans() { return Collections.unmodifiableMap(activeLoans); }
}

// ── Library ───────────────────────────────────────────────────────────────────

class Library {
    private final String          name;
    private final BookCatalog     catalog;
    private final FineCalculator  fineCalculator;
    private final Map<String, Member> members = new HashMap<>();
    private final Map<String, Loan>   memberLoans = new HashMap<>();
    // memberId → loanId list (simplified: memberId → most recent loan)

    Library(String name, FineCalculator fineCalculator) {
        this.name           = Objects.requireNonNull(name);
        this.catalog        = new BookCatalog();
        this.fineCalculator = Objects.requireNonNull(fineCalculator);
    }

    // ── Catalog Management ────────────────────────────────────────────────────

    public void addBook(Book book) { catalog.addBook(book); }

    public void addCopy(ISBN isbn, String barcode) {
        catalog.addCopy(isbn, new BookCopy(barcode, isbn));
    }

    public void registerMember(Member member) {
        Objects.requireNonNull(member);
        members.put(member.getMemberId(), member);
        System.out.println("✅ Registered: " + member);
    }

    // ── Borrowing ─────────────────────────────────────────────────────────────

    public Loan borrowBook(String memberId, ISBN isbn) {
        Member member = getMember(memberId);
        Book   book   = catalog.getBook(isbn);

        // Validate no outstanding fine
        if (member.hasFine())
            throw new OutstandingFineException(memberId, new Fine(member.getOutstandingFine(), 0));

        // Validate borrow limit
        long currentLoans = catalog.getActiveLoans().values().stream()
            .filter(l -> l.getMember().getMemberId().equals(memberId))
            .count();
        if (currentLoans >= member.getBorrowLimit())
            throw new BorrowLimitExceededException(memberId, member.getBorrowLimit());

        // Find available copy
        BookCopy copy = book.findAvailableCopy()
            .orElseThrow(() -> new BookNotAvailableException(isbn.value()));

        copy.setStatus(BookStatus.BORROWED);
        Loan loan = new Loan(member, copy, member.getLoanDurationDays());
        catalog.recordLoan(loan);

        System.out.printf("📚 %s borrowed '%s' | Due: %s | %s%n",
            member.getName(), book.getTitle(), loan.getDueDate(), loan.getLoanId());
        return loan;
    }

    public Fine returnBook(String barcode) {
        Loan loan = catalog.getActiveLoan(barcode)
            .orElseThrow(() -> new LibraryException("No active loan for barcode: " + barcode));

        LocalDate returnDate = LocalDate.now();
        loan.returnBook(returnDate);
        loan.getCopy().setStatus(BookStatus.AVAILABLE);
        catalog.removeLoan(barcode);

        // Calculate fine
        Fine fine = fineCalculator.calculate(loan.getDueDate(), returnDate);
        if (fine.isOwed()) {
            loan.getMember().addFine(fine.amountRupees());
            System.out.printf("⚠ Fine applied to %s: %s%n", loan.getMember().getName(), fine);
        }

        System.out.printf("✅ Returned: '%s' by %s | %s%n",
            loan.getCopy().getIsbn(), loan.getMember().getName(), fine);

        // Notify next reservation holder
        notifyNextReservation(loan.getCopy().getIsbn());

        return fine;
    }

    // ── Reservation ───────────────────────────────────────────────────────────

    public Reservation reserveBook(String memberId, ISBN isbn) {
        Member member = getMember(memberId);
        catalog.getBook(isbn); // validate book exists

        if (catalog.hasReservation(isbn, memberId))
            throw new LibraryException("Member " + memberId + " already has a reservation for " + isbn);

        // If available now, suggest borrowing instead
        Book book = catalog.getBook(isbn);
        if (book.availableCopiesCount() > 0)
            System.out.printf("ℹ '%s' is available now — borrow directly!%n", book.getTitle());

        Reservation res = new Reservation(member, isbn);
        catalog.addReservation(res);
        System.out.printf("🔖 Reserved: '%s' for %s | %s%n",
            book.getTitle(), member.getName(), res.getReservationId());
        return res;
    }

    private void notifyNextReservation(ISBN isbn) {
        catalog.nextReservation(isbn).ifPresent(res ->
            res.notify("'" + catalog.getBook(isbn).getTitle() +
                "' is now available. Visit the library to collect it."));
    }

    // ── Fines ─────────────────────────────────────────────────────────────────

    public void payFine(String memberId, double amount) {
        Member member = getMember(memberId);
        member.payFine(amount);
        System.out.printf("💰 Fine payment: %s paid ₹%.2f | Remaining: ₹%.2f%n",
            member.getName(), amount, member.getOutstandingFine());
    }

    // ── Search ────────────────────────────────────────────────────────────────

    public List<Book> searchByTitle(String title)   { return catalog.searchByTitle(title); }
    public List<Book> searchByAuthor(String author) { return catalog.searchByAuthor(author); }
    public List<Book> searchByGenre(String genre)   { return catalog.searchByGenre(genre); }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Member getMember(String memberId) {
        Member m = members.get(memberId);
        if (m == null) throw new MemberNotFoundException(memberId);
        return m;
    }

    public void displayStatus() {
        System.out.println("\n═══ " + name + " ═══");
        System.out.println("Members: " + members.size());
        System.out.println("Active loans: " + catalog.getActiveLoans().size());
        catalog.getActiveLoans().values().stream()
            .filter(Loan::isOverdue)
            .forEach(l -> System.out.println("  ⚠ OVERDUE: " + l));
    }
}

// ── Main Demo ─────────────────────────────────────────────────────────────────

public class LibraryDemo {
    public static void main(String[] args) {
        Library library = new Library("Central City Library", new StandardFineCalculator());

        // Add books
        ISBN javaIsbn = new ISBN("9780134685991");
        Book javaBook = new Book(javaIsbn, "Effective Java",
            List.of(new Author("Joshua", "Bloch")), "Programming", 2018);
        library.addBook(javaBook);
        library.addCopy(javaIsbn, "BC-001");
        library.addCopy(javaIsbn, "BC-002");

        ISBN cleanIsbn = new ISBN("9780132350884");
        Book cleanCode = new Book(cleanIsbn, "Clean Code",
            List.of(new Author("Robert", "Martin")), "Programming", 2008);
        library.addBook(cleanCode);
        library.addCopy(cleanIsbn, "BC-003");

        // Register members
        Member alice = new Member("M001", "Alice Kumar", "alice@example.com", MemberType.STUDENT);
        Member bob   = new Member("M002", "Bob Sharma",  "bob@example.com",   MemberType.FACULTY);
        library.registerMember(alice);
        library.registerMember(bob);

        // Search
        System.out.println("\n=== Search: 'java' ===");
        library.searchByTitle("java").forEach(System.out::println);

        // Borrow
        System.out.println("\n=== Borrowing ===");
        Loan loan1 = library.borrowBook("M001", javaIsbn);
        Loan loan2 = library.borrowBook("M002", cleanIsbn);

        // Try to borrow same book — no copies left
        library.addCopy(javaIsbn, "BC-004");
        Loan loan3 = library.borrowBook("M002", javaIsbn);  // uses BC-004

        // Reserve when out of copies
        System.out.println("\n=== Reservation ===");
        library.reserveBook("M001", cleanIsbn);  // Alice reserves Clean Code

        // Return + fine simulation
        System.out.println("\n=== Return ===");
        library.returnBook("BC-001");  // Alice returns Java book
        library.returnBook("BC-003");  // Bob returns Clean Code → notifies Alice

        // Overdue fine test (by manually checking)
        System.out.println("\n=== Fine Payment ===");
        alice.addFine(10.0);  // simulate overdue fine
        System.out.println("Alice outstanding fine: ₹" + alice.getOutstandingFine());
        library.payFine("M001", 10.0);

        // Borrow limit test
        System.out.println("\n=== Borrow Limit ===");
        try {
            library.borrowBook("M001", javaIsbn);
            library.borrowBook("M001", javaIsbn);
            library.borrowBook("M001", javaIsbn);
            library.borrowBook("M001", javaIsbn);  // exceeds STUDENT limit of 3
        } catch (BorrowLimitExceededException e) {
            System.out.println("❌ " + e.getMessage());
        } catch (BookNotAvailableException e) {
            System.out.println("❌ " + e.getMessage());
        }

        library.displayStatus();
    }
}
```

## Extension Q&A

**Q: How do you support e-books with concurrent readers?**
Add `EBook extends Book` with `maxConcurrentReaders` field. `EBookLoan` doesn't change copy status to BORROWED — it increments an `activeSessions` counter. Fine applies only to physical books. `EBook.findAvailableCopy()` returns the same virtual copy if `activeSessions < maxConcurrentReaders`.

**Q: How do you send overdue reminders automatically?**
A scheduled job (`ScheduledExecutorService` or Spring `@Scheduled`) runs daily. It queries `catalog.getActiveLoans()`, filters overdue loans, and sends reminder emails. The `NotificationService` interface (Strategy) can be implemented as `EmailNotificationService` or `SmsNotificationService`.

**Q: How would you add a librarian role with admin access?**
Add `Role` enum (MEMBER, LIBRARIAN, ADMIN). Wrap all write operations in an authorization check: `authorizationService.requireRole(caller, Role.LIBRARIAN)`. The `Library` class methods accept a `Caller` parameter. Librarians can add books, mark copies as LOST/DAMAGED, and waive fines.
