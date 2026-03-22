package lld.library;
import java.util.Objects;

public class Member {
    private final String     memberId;
    private final String     name;
    private final String     email;
    private final MemberType type;
    private final int        borrowLimit;
    private final int        loanDurationDays;
    private double           outstandingFine = 0.0;

    public Member(String memberId, String name, String email, MemberType type) {
        this.memberId         = Objects.requireNonNull(memberId);
        this.name             = Objects.requireNonNull(name);
        this.email            = Objects.requireNonNull(email);
        this.type             = Objects.requireNonNull(type);
        this.borrowLimit      = switch (type) { case STUDENT -> 3; case FACULTY -> 10; case PUBLIC -> 2; };
        this.loanDurationDays = switch (type) { case STUDENT -> 14; case FACULTY -> 30; case PUBLIC -> 7; };
        if (name.isBlank())       throw new IllegalArgumentException("Name cannot be blank");
        if (!email.contains("@")) throw new IllegalArgumentException("Invalid email: " + email);
    }

    public String     getMemberId()         { return memberId; }
    public String     getName()             { return name; }
    public String     getEmail()            { return email; }
    public MemberType getType()             { return type; }
    public int        getBorrowLimit()      { return borrowLimit; }
    public int        getLoanDurationDays() { return loanDurationDays; }
    public double     getOutstandingFine()  { return outstandingFine; }
    public boolean    hasFine()             { return outstandingFine > 0; }

    public void addFine(double amount) {
        if (amount < 0) throw new IllegalArgumentException("Fine cannot be negative");
        outstandingFine += amount;
    }

    public void payFine(double amount) {
        if (amount <= 0)          throw new IllegalArgumentException("Payment must be positive");
        if (amount > outstandingFine) throw new LibraryException("Payment exceeds outstanding fine");
        outstandingFine -= amount;
    }

    @Override public String toString() {
        return String.format("Member[%s %s (%s) fine=Rs.%.2f]", memberId, name, type, outstandingFine);
    }
}
