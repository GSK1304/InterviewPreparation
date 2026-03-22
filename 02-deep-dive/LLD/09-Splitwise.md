# LLD — Splitwise (Expense Splitting App) — Complete Java 21

## Design Summary
| Aspect | Decision |
|--------|----------|
| Split types | **Strategy** — EqualSplit, ExactSplit, PercentSplit, ShareSplit |
| Balance tracking | `Map<String, Map<String, Long>>` — userId → (owedTo → amountPaise) |
| Debt simplification | Greedy algorithm — reduces N² transactions to minimum |
| Money | `long` paise — no floating point |
| Groups | Users can belong to multiple groups; expense can be group-scoped |
| Patterns | Strategy (split), Observer (notification on balance change) |

## Complete Solution

```java
package lld.splitwise;

import java.time.Instant;
import java.util.*;
import java.util.stream.*;

// ── Money (paise to avoid float errors) ──────────────────────────────────────

record Money(long paise) {
    static final Money ZERO = new Money(0);

    Money {
        if (paise < 0) throw new IllegalArgumentException("Money cannot be negative");
    }

    static Money ofRupees(double rupees) {
        if (rupees < 0) throw new IllegalArgumentException("Amount cannot be negative");
        return new Money(Math.round(rupees * 100));
    }

    Money add(Money other)      { return new Money(paise + other.paise); }
    Money subtract(Money other) {
        if (other.paise > paise)
            throw new IllegalArgumentException("Cannot subtract larger amount");
        return new Money(paise - other.paise);
    }

    boolean isZero()                         { return paise == 0; }
    boolean isGreaterThan(Money other)       { return paise > other.paise; }
    double  toRupees()                       { return paise / 100.0; }
    @Override public String toString()       { return String.format("₹%.2f", toRupees()); }
}

// ── Enums ─────────────────────────────────────────────────────────────────────

enum SplitType { EQUAL, EXACT, PERCENT, SHARE }
enum ExpenseCategory { FOOD, TRAVEL, ENTERTAINMENT, UTILITIES, RENT, OTHER }

// ── Exceptions ────────────────────────────────────────────────────────────────

class SplitwiseException extends RuntimeException {
    SplitwiseException(String msg) { super(msg); }
}

class UserNotFoundException extends SplitwiseException {
    UserNotFoundException(String id) { super("User not found: " + id); }
}

class GroupNotFoundException extends SplitwiseException {
    GroupNotFoundException(String id) { super("Group not found: " + id); }
}

class InvalidSplitException extends SplitwiseException {
    InvalidSplitException(String msg) { super("Invalid split: " + msg); }
}

// ── Split Strategies ──────────────────────────────────────────────────────────

interface SplitStrategy {
    /**
     * Calculate how much each user owes.
     * @param participants list of user IDs who owe
     * @param totalAmount  total expense amount
     * @param params       strategy-specific params (percentages, exact amounts, etc.)
     * @return map of userId → amount they owe
     */
    Map<String, Money> calculate(List<String> participants, Money totalAmount,
                                 Map<String, Object> params);
}

class EqualSplitStrategy implements SplitStrategy {
    @Override
    public Map<String, Money> calculate(List<String> participants, Money totalAmount,
                                        Map<String, Object> params) {
        if (participants.isEmpty())
            throw new InvalidSplitException("At least one participant required");

        int    n           = participants.size();
        long   perPersonP  = totalAmount.paise() / n;
        long   remainder   = totalAmount.paise() % n;

        Map<String, Money> result = new LinkedHashMap<>();
        for (int i = 0; i < participants.size(); i++) {
            // Distribute remainder paise to first 'remainder' participants
            long share = perPersonP + (i < remainder ? 1 : 0);
            result.put(participants.get(i), new Money(share));
        }
        return result;
    }
}

class ExactSplitStrategy implements SplitStrategy {
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Money> calculate(List<String> participants, Money totalAmount,
                                        Map<String, Object> params) {
        Map<String, Money> exactAmounts = (Map<String, Money>) params.get("exactAmounts");
        if (exactAmounts == null)
            throw new InvalidSplitException("exactAmounts param required for EXACT split");

        // Validate all participants have an exact amount
        for (String userId : participants) {
            if (!exactAmounts.containsKey(userId))
                throw new InvalidSplitException("No exact amount for user: " + userId);
        }

        // Validate total matches
        Money sum = exactAmounts.values().stream()
            .reduce(Money.ZERO, Money::add);
        if (sum.paise() != totalAmount.paise())
            throw new InvalidSplitException(
                "Exact amounts sum " + sum + " doesn't match total " + totalAmount);

        return new LinkedHashMap<>(exactAmounts);
    }
}

class PercentSplitStrategy implements SplitStrategy {
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Money> calculate(List<String> participants, Money totalAmount,
                                        Map<String, Object> params) {
        Map<String, Double> percentages = (Map<String, Double>) params.get("percentages");
        if (percentages == null)
            throw new InvalidSplitException("percentages param required for PERCENT split");

        double totalPercent = percentages.values().stream()
            .mapToDouble(Double::doubleValue).sum();
        if (Math.abs(totalPercent - 100.0) > 0.01)
            throw new InvalidSplitException(
                "Percentages must sum to 100, got: " + totalPercent);

        Map<String, Money> result     = new LinkedHashMap<>();
        long               allocated  = 0;
        List<String>       keys       = new ArrayList<>(percentages.keySet());

        for (int i = 0; i < keys.size(); i++) {
            String userId = keys.get(i);
            long share;
            if (i == keys.size() - 1) {
                // Last person gets the remainder to avoid rounding loss
                share = totalAmount.paise() - allocated;
            } else {
                share = Math.round(totalAmount.paise() * percentages.get(userId) / 100.0);
                allocated += share;
            }
            result.put(userId, new Money(share));
        }
        return result;
    }
}

class ShareSplitStrategy implements SplitStrategy {
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Money> calculate(List<String> participants, Money totalAmount,
                                        Map<String, Object> params) {
        Map<String, Integer> shares = (Map<String, Integer>) params.get("shares");
        if (shares == null)
            throw new InvalidSplitException("shares param required for SHARE split");

        int totalShares = shares.values().stream().mapToInt(Integer::intValue).sum();
        if (totalShares <= 0)
            throw new InvalidSplitException("Total shares must be positive");

        Map<String, Money> result    = new LinkedHashMap<>();
        long               allocated = 0;
        List<String>       keys      = new ArrayList<>(shares.keySet());

        for (int i = 0; i < keys.size(); i++) {
            String userId = keys.get(i);
            long share;
            if (i == keys.size() - 1) {
                share = totalAmount.paise() - allocated;
            } else {
                share = Math.round((double) totalAmount.paise() * shares.get(userId) / totalShares);
                allocated += share;
            }
            result.put(userId, new Money(share));
        }
        return result;
    }
}

// ── Domain Objects ────────────────────────────────────────────────────────────

record User(String userId, String name, String email) {
    User {
        Objects.requireNonNull(userId, "User ID required");
        Objects.requireNonNull(name,   "Name required");
        Objects.requireNonNull(email,  "Email required");
        if (userId.isBlank()) throw new IllegalArgumentException("User ID cannot be blank");
        if (name.isBlank())   throw new IllegalArgumentException("Name cannot be blank");
        if (!email.contains("@")) throw new IllegalArgumentException("Invalid email: " + email);
    }
}

class Expense {
    private static int counter = 1000;

    private final String              expenseId;
    private final String              description;
    private final Money               totalAmount;
    private final String              paidByUserId;
    private final Map<String, Money>  owedAmounts;   // userId → how much they owe
    private final ExpenseCategory     category;
    private final String              groupId;       // nullable for non-group expenses
    private final Instant             createdAt;

    Expense(String description, Money totalAmount, String paidByUserId,
            Map<String, Money> owedAmounts, ExpenseCategory category, String groupId) {
        this.expenseId    = "EXP-" + counter++;
        this.description  = Objects.requireNonNull(description);
        this.totalAmount  = Objects.requireNonNull(totalAmount);
        this.paidByUserId = Objects.requireNonNull(paidByUserId);
        this.owedAmounts  = Collections.unmodifiableMap(Objects.requireNonNull(owedAmounts));
        this.category     = Objects.requireNonNull(category);
        this.groupId      = groupId;
        this.createdAt    = Instant.now();
        if (description.isBlank())
            throw new IllegalArgumentException("Description cannot be blank");
        if (totalAmount.isZero())
            throw new IllegalArgumentException("Expense amount cannot be zero");
    }

    public String              getExpenseId()    { return expenseId; }
    public String              getDescription()  { return description; }
    public Money               getTotalAmount()  { return totalAmount; }
    public String              getPaidByUserId() { return paidByUserId; }
    public Map<String, Money>  getOwedAmounts()  { return owedAmounts; }
    public ExpenseCategory     getCategory()     { return category; }
    public Optional<String>    getGroupId()      { return Optional.ofNullable(groupId); }

    @Override public String toString() {
        return String.format("[%s] '%s' %s paid by user %s",
            expenseId, description, totalAmount, paidByUserId);
    }
}

class Group {
    private static int counter = 100;

    private final String       groupId;
    private final String       name;
    private final Set<String>  memberIds = new LinkedHashSet<>();  // ordered

    Group(String name, String creatorId) {
        this.groupId = "GRP-" + counter++;
        this.name    = Objects.requireNonNull(name);
        if (name.isBlank()) throw new IllegalArgumentException("Group name cannot be blank");
        addMember(creatorId);
    }

    void addMember(String userId) {
        Objects.requireNonNull(userId, "User ID required");
        memberIds.add(userId);
    }

    void removeMember(String userId) { memberIds.remove(userId); }

    boolean hasMember(String userId) { return memberIds.contains(userId); }

    public String      getGroupId()  { return groupId; }
    public String      getName()     { return name; }
    public Set<String> getMemberIds(){ return Collections.unmodifiableSet(memberIds); }
}

// ── Settlement (minimised transactions) ──────────────────────────────────────

record Settlement(String fromUserId, String toUserId, Money amount) {
    Settlement {
        Objects.requireNonNull(fromUserId, "From user required");
        Objects.requireNonNull(toUserId,   "To user required");
        Objects.requireNonNull(amount,     "Amount required");
        if (fromUserId.equals(toUserId))
            throw new IllegalArgumentException("Cannot settle with yourself");
        if (amount.isZero())
            throw new IllegalArgumentException("Settlement amount cannot be zero");
    }
    @Override public String toString() {
        return String.format("%s owes %s → %s", fromUserId, toUserId, amount);
    }
}

// ── Balance Ledger ────────────────────────────────────────────────────────────

class BalanceLedger {
    // netBalance[A][B] = positive means B owes A; negative means A owes B
    private final Map<String, Map<String, Long>> ledger = new HashMap<>();

    /** Record that `owerId` owes `creditorId` the given amount */
    void addDebt(String owerId, String creditorId, Money amount) {
        if (owerId.equals(creditorId)) return;
        adjust(owerId,     creditorId, -amount.paise());
        adjust(creditorId, owerId,      amount.paise());
    }

    /** Record that `payerId` settled their debt with `creditorId` */
    void settle(String payerId, String creditorId, Money amount) {
        adjust(payerId,    creditorId,  amount.paise());
        adjust(creditorId, payerId,    -amount.paise());
    }

    private void adjust(String from, String to, long delta) {
        ledger.computeIfAbsent(from, k -> new HashMap<>())
              .merge(to, delta, Long::sum);
    }

    /** net balance: positive = other owes you; negative = you owe other */
    long getNetBalance(String userId, String otherId) {
        return ledger.getOrDefault(userId, Collections.emptyMap())
                     .getOrDefault(otherId, 0L);
    }

    /** All balances for a user (positive = owed to them, negative = they owe) */
    Map<String, Long> getUserBalances(String userId) {
        return Collections.unmodifiableMap(
            ledger.getOrDefault(userId, Collections.emptyMap()));
    }

    /**
     * Simplify debts: minimum number of transactions to settle everything.
     * Algorithm: sum net positions, greedily match max creditor with max debtor.
     */
    List<Settlement> simplifyDebts(Set<String> userIds) {
        // Compute net position for each user
        Map<String, Long> net = new HashMap<>();
        for (String userId : userIds) {
            long total = getUserBalances(userId).values().stream()
                .mapToLong(Long::longValue).sum();
            net.put(userId, total);
        }

        // Separate creditors (positive) and debtors (negative)
        PriorityQueue<Map.Entry<String, Long>> creditors =
            new PriorityQueue<>((a, b) -> Long.compare(b.getValue(), a.getValue()));
        PriorityQueue<Map.Entry<String, Long>> debtors =
            new PriorityQueue<>((a, b) -> Long.compare(a.getValue(), b.getValue()));

        for (var entry : net.entrySet()) {
            if (entry.getValue() > 0) creditors.add(entry);
            else if (entry.getValue() < 0) debtors.add(entry);
        }

        List<Settlement> settlements = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            var creditor = creditors.poll();
            var debtor   = debtors.poll();
            long amount  = Math.min(creditor.getValue(), -debtor.getValue());
            settlements.add(new Settlement(debtor.getKey(), creditor.getKey(), new Money(amount)));
            long creditorRemaining = creditor.getValue() - amount;
            long debtorRemaining   = debtor.getValue()   + amount;
            if (creditorRemaining > 0) creditors.add(Map.entry(creditor.getKey(), creditorRemaining));
            if (debtorRemaining   < 0) debtors.add(Map.entry(debtor.getKey(),   debtorRemaining));
        }
        return settlements;
    }
}

// ── Split Strategy Factory ────────────────────────────────────────────────────

class SplitStrategyFactory {
    private static final Map<SplitType, SplitStrategy> STRATEGIES = Map.of(
        SplitType.EQUAL,   new EqualSplitStrategy(),
        SplitType.EXACT,   new ExactSplitStrategy(),
        SplitType.PERCENT, new PercentSplitStrategy(),
        SplitType.SHARE,   new ShareSplitStrategy()
    );

    static SplitStrategy get(SplitType type) {
        SplitStrategy strategy = STRATEGIES.get(type);
        if (strategy == null)
            throw new SplitwiseException("No strategy for split type: " + type);
        return strategy;
    }
}

// ── Splitwise App ─────────────────────────────────────────────────────────────

class SplitwiseApp {
    private final Map<String, User>    users    = new LinkedHashMap<>();
    private final Map<String, Group>   groups   = new LinkedHashMap<>();
    private final List<Expense>        expenses = new ArrayList<>();
    private final BalanceLedger        ledger   = new BalanceLedger();

    // ── User Management ───────────────────────────────────────────────────────

    public User addUser(String userId, String name, String email) {
        if (users.containsKey(userId))
            throw new SplitwiseException("User already exists: " + userId);
        User user = new User(userId, name, email);
        users.put(userId, user);
        System.out.println("👤 Registered: " + name);
        return user;
    }

    public Group createGroup(String name, String creatorId) {
        validateUser(creatorId);
        Group group = new Group(name, creatorId);
        groups.put(group.getGroupId(), group);
        System.out.println("👥 Group created: " + name + " (" + group.getGroupId() + ")");
        return group;
    }

    public void addToGroup(String groupId, String userId) {
        validateUser(userId);
        Group group = getGroup(groupId);
        group.addMember(userId);
        System.out.println("  +" + users.get(userId).name() + " added to " + group.getName());
    }

    // ── Expense Adding ────────────────────────────────────────────────────────

    public Expense addExpense(String description, double amountRupees,
                              String paidByUserId, List<String> participants,
                              SplitType splitType, Map<String, Object> splitParams,
                              ExpenseCategory category, String groupId) {
        validateUser(paidByUserId);
        participants.forEach(this::validateUser);

        if (!participants.contains(paidByUserId))
            participants = new ArrayList<>(participants) {{ add(paidByUserId); }};

        Money total    = Money.ofRupees(amountRupees);
        SplitStrategy strategy = SplitStrategyFactory.get(splitType);
        Map<String, Money> owedAmounts = strategy.calculate(participants, total, splitParams);

        Expense expense = new Expense(description, total, paidByUserId,
            owedAmounts, category, groupId);
        expenses.add(expense);

        // Update ledger: everyone in owedAmounts owes paidByUserId
        // EXCEPT paidByUserId themselves (their share is already "paid")
        owedAmounts.forEach((userId, amount) -> {
            if (!userId.equals(paidByUserId)) {
                ledger.addDebt(userId, paidByUserId, amount);
            }
        });

        System.out.printf("💸 Added: %s%n", expense);
        owedAmounts.forEach((uid, amt) -> {
            if (!uid.equals(paidByUserId))
                System.out.printf("   %s owes %s → %s%n",
                    users.get(uid).name(), users.get(paidByUserId).name(), amt);
        });
        return expense;
    }

    /** Shorthand: equal split */
    public Expense addExpenseEqual(String description, double amount,
                                   String paidBy, List<String> participants) {
        return addExpense(description, amount, paidBy, participants,
            SplitType.EQUAL, Collections.emptyMap(), ExpenseCategory.OTHER, null);
    }

    // ── Settlement ────────────────────────────────────────────────────────────

    public void settle(String payerId, String receiverId, double amountRupees) {
        validateUser(payerId);
        validateUser(receiverId);
        Money amount = Money.ofRupees(amountRupees);
        ledger.settle(payerId, receiverId, amount);
        System.out.printf("✅ %s paid %s → %s%n",
            users.get(payerId).name(), users.get(receiverId).name(), amount);
    }

    // ── Balance Display ───────────────────────────────────────────────────────

    public void showBalances(String userId) {
        validateUser(userId);
        User user = users.get(userId);
        System.out.println("\n─── Balances for " + user.name() + " ───");
        Map<String, Long> balances = ledger.getUserBalances(userId);
        boolean hasAny = false;
        for (var entry : balances.entrySet()) {
            if (entry.getValue() == 0) continue;
            hasAny = true;
            User other = users.get(entry.getKey());
            if (other == null) continue;
            if (entry.getValue() > 0) {
                System.out.printf("  %s owes you %s%n",
                    other.name(), new Money(entry.getValue()));
            } else {
                System.out.printf("  You owe %s %s%n",
                    other.name(), new Money(-entry.getValue()));
            }
        }
        if (!hasAny) System.out.println("  All settled up! ✅");
    }

    public void showAllBalances() {
        System.out.println("\n═══ All Balances ═══");
        users.keySet().forEach(this::showBalances);
    }

    public void showSimplifiedSettlements() {
        System.out.println("\n═══ Simplified Settlements ═══");
        List<Settlement> settlements = ledger.simplifyDebts(users.keySet());
        if (settlements.isEmpty()) {
            System.out.println("  Everyone is settled up! ✅");
        } else {
            settlements.forEach(s -> System.out.printf("  %s pays %s → %s%n",
                users.getOrDefault(s.fromUserId(), new User(s.fromUserId(), s.fromUserId(), "x@x")).name(),
                users.getOrDefault(s.toUserId(),   new User(s.toUserId(),   s.toUserId(),   "x@x")).name(),
                s.amount()));
        }
    }

    public void showGroupBalances(String groupId) {
        Group group = getGroup(groupId);
        System.out.println("\n─── Group: " + group.getName() + " ───");
        List<Settlement> settlements = ledger.simplifyDebts(group.getMemberIds());
        settlements.forEach(s -> System.out.printf("  %s → %s: %s%n",
            users.get(s.fromUserId()).name(),
            users.get(s.toUserId()).name(),
            s.amount()));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validateUser(String userId) {
        if (!users.containsKey(userId)) throw new UserNotFoundException(userId);
    }

    private Group getGroup(String groupId) {
        Group group = groups.get(groupId);
        if (group == null) throw new GroupNotFoundException(groupId);
        return group;
    }
}

// ── Main Demo ─────────────────────────────────────────────────────────────────

public class SplitwiseDemo {
    public static void main(String[] args) {
        SplitwiseApp app = new SplitwiseApp();

        // Register users
        app.addUser("U1", "Alice", "alice@example.com");
        app.addUser("U2", "Bob",   "bob@example.com");
        app.addUser("U3", "Carol", "carol@example.com");
        app.addUser("U4", "Dave",  "dave@example.com");

        // Create a trip group
        Group trip = app.createGroup("Goa Trip", "U1");
        app.addToGroup(trip.getGroupId(), "U2");
        app.addToGroup(trip.getGroupId(), "U3");
        app.addToGroup(trip.getGroupId(), "U4");

        System.out.println("\n=== Expense 1: Equal Split ===");
        app.addExpenseEqual("Hotel", 4000, "U1", List.of("U1","U2","U3","U4"));

        System.out.println("\n=== Expense 2: Exact Split ===");
        app.addExpense("Dinner", 1500, "U2",
            List.of("U1","U2","U3"),
            SplitType.EXACT, Map.of("exactAmounts", Map.of(
                "U1", Money.ofRupees(500),
                "U2", Money.ofRupees(700),
                "U3", Money.ofRupees(300)
            )),
            ExpenseCategory.FOOD, trip.getGroupId());

        System.out.println("\n=== Expense 3: Percent Split ===");
        app.addExpense("Taxi", 600, "U3",
            List.of("U1","U2","U3","U4"),
            SplitType.PERCENT, Map.of("percentages", Map.of(
                "U1", 25.0, "U2", 25.0, "U3", 25.0, "U4", 25.0
            )),
            ExpenseCategory.TRAVEL, trip.getGroupId());

        System.out.println("\n=== Expense 4: Share Split ===");
        app.addExpense("Groceries", 900, "U4",
            List.of("U1","U2","U3","U4"),
            SplitType.SHARE, Map.of("shares", Map.of(
                "U1", 2, "U2", 2, "U3", 1, "U4", 1
            )),
            ExpenseCategory.FOOD, null);

        app.showAllBalances();
        app.showSimplifiedSettlements();

        System.out.println("\n=== Partial Settlement ===");
        app.settle("U2", "U1", 500);
        app.showSimplifiedSettlements();

        // Error cases
        System.out.println("\n=== Error Cases ===");
        try {
            app.addExpense("Bad split", 100, "U1", List.of("U1","U2"),
                SplitType.EXACT, Map.of("exactAmounts", Map.of(
                    "U1", Money.ofRupees(60),
                    "U2", Money.ofRupees(60)   // sum = 120 ≠ 100
                )),
                ExpenseCategory.OTHER, null);
        } catch (InvalidSplitException e) {
            System.out.println("❌ " + e.getMessage());
        }

        try {
            app.addExpense("Invalid percent", 100, "U1", List.of("U1","U2"),
                SplitType.PERCENT, Map.of("percentages", Map.of("U1", 60.0, "U2", 60.0)),
                ExpenseCategory.OTHER, null);
        } catch (InvalidSplitException e) {
            System.out.println("❌ " + e.getMessage());
        }
    }
}
```

## Extension Q&A

**Q: How do you add recurring expenses (monthly rent)?**
Add `RecurringExpense` with a `frequency: Duration` and `nextDueDate: LocalDate`. A scheduled job queries for due recurring expenses and auto-creates new `Expense` instances, applying the same split configuration. Store `RecurringExpense` separately from one-time `Expense` with a `templateExpenseId` link.

**Q: How do you handle currency conversion (multi-currency trips)?**
Replace `Money(long paise)` with `Money(long minorUnit, Currency currency)`. All amounts stored in original currency. On balance display, convert to the user's preferred currency using an injected `ExchangeRateProvider`. Ledger stores raw currency-tagged amounts and converts at read time.

**Q: What is the time complexity of the debt simplification algorithm?**
O(N log N) — sorting net positions into priority queues is O(N log N), then the greedy matching is O(N) iterations. Optimal: Splitwise's actual algorithm is NP-hard for the truly minimum-transactions problem, but this greedy approach gives a near-optimal practical result.
