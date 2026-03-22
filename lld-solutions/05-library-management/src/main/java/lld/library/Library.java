package lld.library;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class Library {
    private final String             name;
    private final FineCalculator     fineCalculator;
    private final Map<ISBN, Book>    books        = new LinkedHashMap<>();
    private final Map<String, Member> members     = new HashMap<>();
    private final Map<String, Loan>  activeLoans  = new HashMap<>(); // barcode -> loan
    private final Map<ISBN, Queue<Reservation>> reservations = new HashMap<>();

    public Library(String name, FineCalculator fineCalculator) {
        this.name           = Objects.requireNonNull(name);
        this.fineCalculator = Objects.requireNonNull(fineCalculator);
    }

    public void addBook(Book book)   { books.put(Objects.requireNonNull(book).getIsbn(), book); }
    public void addCopy(ISBN isbn, String barcode) {
        getBook(isbn).addCopy(new BookCopy(barcode, isbn));
    }
    public void registerMember(Member m) {
        members.put(Objects.requireNonNull(m).getMemberId(), m);
        System.out.println("Registered: " + m);
    }

    public Loan borrowBook(String memberId, ISBN isbn) {
        Member member = getMember(memberId);
        Book   book   = getBook(isbn);
        if (member.hasFine())
            throw new OutstandingFineException(memberId, member.getOutstandingFine());
        long currentLoans = activeLoans.values().stream()
            .filter(l -> l.getMember().getMemberId().equals(memberId)).count();
        if (currentLoans >= member.getBorrowLimit())
            throw new BorrowLimitExceededException(memberId, member.getBorrowLimit());
        BookCopy copy = book.findAvailableCopy()
            .orElseThrow(() -> new BookNotAvailableException(isbn.value()));
        copy.setStatus(BookStatus.BORROWED);
        Loan loan = new Loan(member, copy, member.getLoanDurationDays());
        activeLoans.put(copy.getBarcode(), loan);
        System.out.printf("Borrowed: %s by %s | Due: %s | %s%n",
            book.getTitle(), member.getName(), loan.getDueDate(), loan.getLoanId());
        return loan;
    }

    public Fine returnBook(String barcode) {
        Loan loan = Optional.ofNullable(activeLoans.remove(barcode))
            .orElseThrow(() -> new LibraryException("No active loan for barcode: " + barcode));
        loan.returnBook(LocalDate.now());
        loan.getCopy().setStatus(BookStatus.AVAILABLE);
        Fine fine = fineCalculator.calculate(loan.getDueDate(), LocalDate.now());
        if (fine.isOwed()) loan.getMember().addFine(fine.amountRupees());
        System.out.printf("Returned: '%s' | %s%n", loan.getCopy().getIsbn(), fine);
        // Notify next reservation
        ISBN isbn = loan.getCopy().getIsbn();
        Queue<Reservation> q = reservations.get(isbn);
        if (q != null && !q.isEmpty()) {
            Reservation res = q.poll();
            res.notifyAvailable(books.get(isbn).getTitle());
        }
        return fine;
    }

    public Reservation reserveBook(String memberId, ISBN isbn) {
        Member member = getMember(memberId);
        getBook(isbn);
        Reservation res = new Reservation(member, isbn);
        reservations.computeIfAbsent(isbn, k -> new LinkedList<>()).add(res);
        System.out.printf("Reserved: '%s' for %s | %s%n",
            books.get(isbn).getTitle(), member.getName(), res.getReservationId());
        return res;
    }

    public void payFine(String memberId, double amount) {
        Member m = getMember(memberId);
        m.payFine(amount);
        System.out.printf("Fine paid: %s paid Rs.%.2f | Remaining: Rs.%.2f%n",
            m.getName(), amount, m.getOutstandingFine());
    }

    public List<Book> searchByTitle(String title) {
        String lower = title.toLowerCase();
        return books.values().stream()
            .filter(b -> b.getTitle().toLowerCase().contains(lower)).collect(Collectors.toList());
    }

    public void displayStatus() {
        System.out.println("\n=== " + name + " ===");
        System.out.println("Members: " + members.size() + " | Active loans: " + activeLoans.size());
        activeLoans.values().stream().filter(Loan::isOverdue)
            .forEach(l -> System.out.println("  OVERDUE: " + l));
    }

    private Book   getBook(ISBN isbn) {
        Book b = books.get(isbn);
        if (b == null) throw new BookNotFoundException(isbn.value()); return b;
    }
    private Member getMember(String id) {
        Member m = members.get(id);
        if (m == null) throw new MemberNotFoundException(id); return m;
    }
}
