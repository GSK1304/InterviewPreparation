package lld.splitwise.dto;
import jakarta.validation.constraints.*;
import lld.splitwise.enums.ExpenseCategory;
import lld.splitwise.enums.SplitType;
import lombok.Data;
import java.util.*;
@Data
public class AddExpenseRequest {
    @NotBlank(message = "Description is required")
    @Size(max = 200, message = "Description max 200 characters")
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Amount must be at least Re.1")
    @DecimalMax(value = "1000000.0", message = "Amount cannot exceed Rs.10 lakh")
    private Double amountRupees;

    @NotBlank(message = "Paid by user ID is required")
    private String paidByUserId;

    @NotEmpty(message = "Participants list cannot be empty")
    @Size(min = 2, max = 20, message = "Need 2-20 participants")
    private List<String> participantIds;

    @NotNull(message = "Split type is required")
    private SplitType splitType;

    private ExpenseCategory category = ExpenseCategory.OTHER;
    private Long groupId;

    // For EXACT split: userId -> amountRupees
    private Map<String, Double> exactAmounts;

    // For PERCENT split: userId -> percentage
    private Map<String, Double> percentages;

    // For SHARE split: userId -> share count
    private Map<String, Integer> shares;
}
