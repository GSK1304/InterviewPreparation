package lld.splitwise.dto;
import lombok.Builder;
import lombok.Data;
import java.util.List;
@Data @Builder
public class BalanceResponse {
    private String userId;
    private String userName;
    private List<BalanceEntry> owes;
    private List<BalanceEntry> owedBy;
    private String netBalance;

    @Data @Builder
    public static class BalanceEntry {
        private String userId;
        private String userName;
        private String amount;
    }
}
