package lld.library;
import java.util.List;

public class LibraryDemo {
    public static void main(String[] args) {
        Library library = new Library("Central City Library", new StandardFineCalculator());

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

        Member alice = new Member("M001", "Alice Kumar", "alice@example.com", MemberType.STUDENT);
        Member bob   = new Member("M002", "Bob Sharma",  "bob@example.com",   MemberType.FACULTY);
        library.registerMember(alice);
        library.registerMember(bob);

        System.out.println("\n=== Search ===");
        library.searchByTitle("java").forEach(System.out::println);

        System.out.println("\n=== Borrow ===");
        Loan loan1 = library.borrowBook("M001", javaIsbn);
        Loan loan2 = library.borrowBook("M002", cleanIsbn);

        System.out.println("\n=== Reserve ===");
        library.reserveBook("M001", cleanIsbn);

        System.out.println("\n=== Return ===");
        library.returnBook("BC-001");
        library.returnBook("BC-003");

        System.out.println("\n=== Fine ===");
        alice.addFine(10.0);
        System.out.println("Alice fine: Rs." + alice.getOutstandingFine());
        library.payFine("M001", 10.0);

        System.out.println("\n=== Borrow Limit ===");
        try {
            library.borrowBook("M001", javaIsbn);
            library.borrowBook("M001", javaIsbn);
            library.borrowBook("M001", javaIsbn);
            library.borrowBook("M001", javaIsbn);
        } catch (BorrowLimitExceededException | BookNotAvailableException e) {
            System.out.println("Error: " + e.getMessage());
        }

        library.displayStatus();
    }
}
