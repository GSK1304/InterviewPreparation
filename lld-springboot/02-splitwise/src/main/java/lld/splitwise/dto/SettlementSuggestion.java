package lld.splitwise.dto;
import lombok.Builder;
import lombok.Data;
import java.util.List;
@Data @Builder
public class SettlementSuggestion {
    private List<Transaction> transactions;
    private String message;
    @Data @Builder
    public static class Transaction {
        private String fromUserId;
        private String fromUserName;
        private String toUserId;
        private String toUserName;
        private String amount;
    }
}
