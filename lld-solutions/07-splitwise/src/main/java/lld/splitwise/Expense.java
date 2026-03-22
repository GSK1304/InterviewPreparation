package lld.splitwise;
import java.time.Instant;
import java.util.*;

public class Expense {
    private static int counter = 1000;
    private final String              expenseId;
    private final String              description;
    private final Money               totalAmount;
    private final String              paidByUserId;
    private final Map<String, Money>  owedAmounts;
    private final ExpenseCategory     category;
    private final String              groupId;
    private final Instant             createdAt;

    public Expense(String description, Money totalAmount, String paidByUserId,
                   Map<String, Money> owedAmounts, ExpenseCategory category, String groupId) {
        this.expenseId    = "EXP-" + counter++;
        this.description  = Objects.requireNonNull(description);
        this.totalAmount  = Objects.requireNonNull(totalAmount);
        this.paidByUserId = Objects.requireNonNull(paidByUserId);
        this.owedAmounts  = Collections.unmodifiableMap(Objects.requireNonNull(owedAmounts));
        this.category     = Objects.requireNonNull(category);
        this.groupId      = groupId;
        this.createdAt    = Instant.now();
        if (description.isBlank()) throw new IllegalArgumentException("Description required");
        if (totalAmount.isZero())  throw new IllegalArgumentException("Amount cannot be zero");
    }

    public String             getExpenseId()    { return expenseId; }
    public String             getDescription()  { return description; }
    public Money              getTotalAmount()  { return totalAmount; }
    public String             getPaidByUserId() { return paidByUserId; }
    public Map<String, Money> getOwedAmounts()  { return owedAmounts; }
    @Override public String toString() {
        return String.format("[%s] '%s' %s paid by %s", expenseId, description, totalAmount, paidByUserId);
    }
}
