package lld.library;
import java.time.*;
import java.util.Objects;

public class Loan {
    private final String    loanId;
    private final Member    member;
    private final BookCopy  copy;
    private final LocalDate borrowDate;
    private final LocalDate dueDate;
    private LocalDate       returnDate;
    private LoanStatus      status;
    private static int counter = 1000;

    public Loan(Member member, BookCopy copy, int loanDurationDays) {
        this.loanId     = "LN" + counter++;
        this.member     = Objects.requireNonNull(member);
        this.copy       = Objects.requireNonNull(copy);
        this.borrowDate = LocalDate.now();
        this.dueDate    = borrowDate.plusDays(loanDurationDays);
        this.status     = LoanStatus.ACTIVE;
    }

    public void returnBook(LocalDate returnDate) {
        Objects.requireNonNull(returnDate);
        if (returnDate.isBefore(borrowDate)) throw new IllegalArgumentException("Return date before borrow date");
        this.returnDate = returnDate;
        this.status     = returnDate.isAfter(dueDate) ? LoanStatus.OVERDUE : LoanStatus.RETURNED;
    }

    public boolean isOverdue() { return LocalDate.now().isAfter(dueDate) && status == LoanStatus.ACTIVE; }

    public String    getLoanId()    { return loanId; }
    public Member    getMember()    { return member; }
    public BookCopy  getCopy()      { return copy; }
    public LocalDate getDueDate()   { return dueDate; }
    public LocalDate getReturnDate(){ return returnDate; }
    public LoanStatus getStatus()  { return status; }

    @Override public String toString() {
        return String.format("Loan[%s %s due=%s status=%s]", loanId, member.getName(), dueDate, status);
    }
}
