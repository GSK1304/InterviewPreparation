package lld.atm;
import java.util.Map;

public class ATMFactory {
    public static ATMMachine create(String atmId, Map<String, BankAccount> accounts) {
        CashCassette c2000 = new CashCassette(2000, 50);
        CashCassette c500  = new CashCassette(500,  100);
        CashCassette c200  = new CashCassette(200,  100);
        CashCassette c100  = new CashCassette(100,  200);
        CashDispenser chain = new Rs2000Dispenser(c2000);
        chain.setNext(new Rs500Dispenser(c500))
             .setNext(new Rs200Dispenser(c200))
             .setNext(new Rs100Dispenser(c100));
        return new ATMMachine(atmId, chain, new TransactionLog(), accounts);
    }
}
