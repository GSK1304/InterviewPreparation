package lld.atm;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class ATMDemo {
    public static void main(String[] args) {
        Map<String, BankAccount> accounts = new HashMap<>();
        accounts.put("1234567890123456", new BankAccount("ACC001", "1234", Money.ofRupees(50_000)));
        accounts.put("9876543210987654", new BankAccount("ACC002", "5678", Money.ofRupees(10_000)));

        ATMMachine atm = ATMFactory.create("ATM-HYD-001", accounts);
        Card card1 = new Card("1234567890123456", "SBI", Instant.now().plusSeconds(365*24*3600));
        Card card2 = new Card("9876543210987654", "HDFC", Instant.now().plusSeconds(365*24*3600));

        System.out.println("\n=== Scenario 1: Successful Withdrawal ===");
        atm.insertCard(card1);
        atm.enterPin("1234");
        atm.checkBalance();
        atm.withdraw(Money.ofRupees(5500));
        atm.checkBalance();
        atm.ejectCard();

        System.out.println("\n=== Scenario 2: Wrong PIN -> Block ===");
        atm.insertCard(card1);
        try { atm.enterPin("0000"); } catch (WrongPINException e) { System.out.println("Error: " + e.getMessage()); }
        try { atm.enterPin("1111"); } catch (WrongPINException e) { System.out.println("Error: " + e.getMessage()); }
        try { atm.enterPin("2222"); } catch (CardBlockedException e) { System.out.println("Error: " + e.getMessage()); }
        atm.ejectCard();

        System.out.println("\n=== Scenario 3: Insufficient Funds ===");
        atm.insertCard(card2);
        atm.enterPin("5678");
        try { atm.withdraw(Money.ofRupees(50_000)); }
        catch (InsufficientFundsException e) { System.out.println("Error: " + e.getMessage()); }
        atm.deposit(Money.ofRupees(5_000));
        atm.ejectCard();

        System.out.println("\n=== Scenario 4: Wrong State ===");
        try { atm.withdraw(Money.ofRupees(1000)); }
        catch (InvalidATMStateException e) { System.out.println("Error: " + e.getMessage()); }

        atm.printLog();
    }
}
