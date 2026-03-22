package lld.splitwise;
import java.util.*;

public class SplitwiseDemo {
    public static void main(String[] args) {
        SplitwiseApp app = new SplitwiseApp();
        app.addUser("U1", "Alice", "alice@example.com");
        app.addUser("U2", "Bob",   "bob@example.com");
        app.addUser("U3", "Carol", "carol@example.com");
        app.addUser("U4", "Dave",  "dave@example.com");

        Group trip = app.createGroup("Goa Trip", "U1");
        app.addToGroup(trip.getGroupId(), "U2");
        app.addToGroup(trip.getGroupId(), "U3");
        app.addToGroup(trip.getGroupId(), "U4");

        System.out.println("\n=== Equal Split ===");
        app.addExpenseEqual("Hotel", 4000, "U1", List.of("U1","U2","U3","U4"));

        System.out.println("\n=== Exact Split ===");
        app.addExpense("Dinner", 1500, "U2", List.of("U1","U2","U3"),
            SplitType.EXACT, Map.of("exactAmounts", Map.of(
                "U1", Money.ofRupees(500), "U2", Money.ofRupees(700), "U3", Money.ofRupees(300)
            )), ExpenseCategory.FOOD, trip.getGroupId());

        System.out.println("\n=== Percent Split ===");
        app.addExpense("Taxi", 600, "U3", List.of("U1","U2","U3","U4"),
            SplitType.PERCENT, Map.of("percentages", Map.of("U1",25.0,"U2",25.0,"U3",25.0,"U4",25.0)),
            ExpenseCategory.TRAVEL, null);

        System.out.println("\n=== Share Split ===");
        app.addExpense("Groceries", 900, "U4", List.of("U1","U2","U3","U4"),
            SplitType.SHARE, Map.of("shares", Map.of("U1",2,"U2",2,"U3",1,"U4",1)),
            ExpenseCategory.FOOD, null);

        app.showBalances("U1");
        app.showSimplifiedSettlements();

        System.out.println("\n=== Partial Settlement ===");
        app.settle("U2", "U1", 500);
        app.showSimplifiedSettlements();

        System.out.println("\n=== Error Cases ===");
        try {
            app.addExpense("Bad", 100, "U1", List.of("U1","U2"),
                SplitType.EXACT, Map.of("exactAmounts", Map.of("U1",Money.ofRupees(60),"U2",Money.ofRupees(60))),
                ExpenseCategory.OTHER, null);
        } catch (InvalidSplitException e) { System.out.println("Error: " + e.getMessage()); }
    }
}
