package lld.splitwise;
import java.util.*;

public class SplitwiseApp {
    private final Map<String, User>  users  = new LinkedHashMap<>();
    private final Map<String, Group> groups = new LinkedHashMap<>();
    private final List<Expense>      expenses = new ArrayList<>();
    private final BalanceLedger      ledger   = new BalanceLedger();

    public User addUser(String userId, String name, String email) {
        if (users.containsKey(userId)) throw new SplitwiseException("User already exists: " + userId);
        User user = new User(userId, name, email);
        users.put(userId, user);
        System.out.println("Registered: " + name);
        return user;
    }

    public Group createGroup(String name, String creatorId) {
        validateUser(creatorId);
        Group group = new Group(name, creatorId);
        groups.put(group.getGroupId(), group);
        System.out.println("Group created: " + name + " (" + group.getGroupId() + ")");
        return group;
    }

    public void addToGroup(String groupId, String userId) {
        validateUser(userId);
        Group g = groups.get(groupId);
        if (g == null) throw new SplitwiseException("Group not found: " + groupId);
        g.addMember(userId);
        System.out.println("  +" + users.get(userId).name() + " added to " + g.getName());
    }

    public Expense addExpense(String desc, double amount, String paidBy,
                              List<String> participants, SplitType splitType,
                              Map<String, Object> params, ExpenseCategory category, String groupId) {
        validateUser(paidBy);
        participants.forEach(this::validateUser);
        if (!participants.contains(paidBy)) {
            participants = new ArrayList<>(participants);
            participants.add(paidBy);
        }
        Money total   = Money.ofRupees(amount);
        Map<String, Money> owed = SplitStrategyFactory.get(splitType).calculate(participants, total, params);
        Expense expense = new Expense(desc, total, paidBy, owed, category, groupId);
        expenses.add(expense);
        owed.forEach((uid, amt) -> {
            if (!uid.equals(paidBy)) ledger.addDebt(uid, paidBy, amt);
        });
        System.out.printf("Added: %s%n", expense);
        owed.forEach((uid, amt) -> { if (!uid.equals(paidBy))
            System.out.printf("  %s owes %s -> %s%n", users.get(uid).name(), users.get(paidBy).name(), amt);
        });
        return expense;
    }

    public Expense addExpenseEqual(String desc, double amount, String paidBy, List<String> participants) {
        return addExpense(desc, amount, paidBy, participants, SplitType.EQUAL, Collections.emptyMap(), ExpenseCategory.OTHER, null);
    }

    public void settle(String payerId, String receiverId, double amount) {
        validateUser(payerId); validateUser(receiverId);
        Money m = Money.ofRupees(amount);
        ledger.settle(payerId, receiverId, m);
        System.out.printf("Settled: %s paid %s -> %s%n", users.get(payerId).name(), users.get(receiverId).name(), m);
    }

    public void showBalances(String userId) {
        validateUser(userId);
        System.out.println("\n--- Balances for " + users.get(userId).name() + " ---");
        boolean any = false;
        for (var e : ledger.getUserBalances(userId).entrySet()) {
            if (e.getValue() == 0) continue;
            any = true;
            User other = users.get(e.getKey());
            if (other == null) continue;
            if (e.getValue() > 0) System.out.printf("  %s owes you %s%n", other.name(), new Money(e.getValue()));
            else                  System.out.printf("  You owe %s %s%n", other.name(), new Money(-e.getValue()));
        }
        if (!any) System.out.println("  All settled up!");
    }

    public void showSimplifiedSettlements() {
        System.out.println("\n=== Simplified Settlements ===");
        List<Settlement> list = ledger.simplifyDebts(users.keySet());
        if (list.isEmpty()) System.out.println("  Everyone is settled up!");
        else list.forEach(s -> System.out.printf("  %s pays %s -> %s%n",
            users.getOrDefault(s.fromUserId(), new User(s.fromUserId(),"?","?@x")).name(),
            users.getOrDefault(s.toUserId(),   new User(s.toUserId(),  "?","?@x")).name(),
            s.amount()));
    }

    private void validateUser(String userId) {
        if (!users.containsKey(userId)) throw new UserNotFoundException(userId);
    }
}
