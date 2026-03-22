package lld.atm.service;
import jakarta.transaction.Transactional;
import lld.atm.dto.*;
import lld.atm.entity.*;
import lld.atm.enums.*;
import lld.atm.exception.ATMException;
import lld.atm.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.*;

@Service @RequiredArgsConstructor
public class ATMService {
    private static final Logger log = LoggerFactory.getLogger(ATMService.class);
    private static final int MAX_PIN_ATTEMPTS = 3;
    private final BankAccountRepository accountRepo;
    private final ATMTransactionRepository txRepo;
    private final CashCassetteRepository cassetteRepo;

    // Stateless session: card validation + PIN each call (session-less REST design)

    @Transactional
    public Map<String,Object> validateCard(InsertCardRequest req) {
        log.info("[ATMService] Card inserted | masked={}", mask(req.getCardNumber()));
        BankAccount account = accountRepo.findByCardNumber(req.getCardNumber())
            .orElseThrow(() -> new ATMException("Card not recognised: " + mask(req.getCardNumber()), HttpStatus.NOT_FOUND));
        if (account.getBlocked()) throw new ATMException("Card is blocked. Contact your bank.", HttpStatus.FORBIDDEN);
        log.info("[ATMService] Card valid | account={} holder={}", account.getAccountNumber(), account.getAccountHolder());
        return Map.of("accountNumber", account.getAccountNumber(), "accountHolder", account.getAccountHolder(), "message", "Card accepted. Please enter PIN.");
    }

    @Transactional
    public Map<String,Object> verifyPin(String accountNumber, EnterPinRequest req) {
        log.info("[ATMService] PIN verification | account={}", accountNumber);
        BankAccount account = getAccount(accountNumber);
        if (account.getBlocked()) throw new ATMException("Card is blocked.", HttpStatus.FORBIDDEN);
        String hashed = Integer.toHexString(req.getPin().hashCode());
        if (!account.getHashedPin().equals(hashed)) {
            account.setFailedAttempts(account.getFailedAttempts() + 1);
            if (account.getFailedAttempts() >= MAX_PIN_ATTEMPTS) {
                account.setBlocked(true); accountRepo.save(account);
                log.warn("[ATMService] Card blocked after {} failed attempts | account={}", MAX_PIN_ATTEMPTS, accountNumber);
                throw new ATMException("Card blocked after " + MAX_PIN_ATTEMPTS + " wrong attempts.", HttpStatus.FORBIDDEN);
            }
            accountRepo.save(account);
            int remaining = MAX_PIN_ATTEMPTS - account.getFailedAttempts();
            log.warn("[ATMService] Wrong PIN | account={} remaining={}", accountNumber, remaining);
            throw new ATMException("Wrong PIN. " + remaining + " attempt(s) remaining.", HttpStatus.UNAUTHORIZED);
        }
        account.setFailedAttempts(0); accountRepo.save(account);
        log.info("[ATMService] PIN verified | account={}", accountNumber);
        return Map.of("status", "authenticated", "message", "PIN verified. You can proceed.");
    }

    @Transactional
    public Map<String,Object> checkBalance(String accountNumber) {
        BankAccount account = getAccount(accountNumber);
        record(accountNumber, mask(account.getCardNumber()), TransactionType.BALANCE_INQUIRY, 0L, TransactionStatus.SUCCESS, null, null);
        log.info("[ATMService] Balance inquiry | account={} balance=Rs.{}", accountNumber, account.getBalancePaise()/100.0);
        return Map.of("accountNumber", accountNumber, "balance", fmt(account.getBalancePaise()), "accountHolder", account.getAccountHolder());
    }

    @Transactional
    public Map<String,Object> withdraw(String accountNumber, WithdrawRequest req) {
        log.info("[ATMService] Withdraw request | account={} amount=Rs.{}", accountNumber, req.getAmountRupees());
        if (req.getAmountRupees() % 100 != 0) throw new ATMException("Amount must be in multiples of Rs.100", HttpStatus.BAD_REQUEST);
        long amountPaise = req.getAmountRupees() * 100;
        BankAccount account = getAccount(accountNumber);
        if (account.getBalancePaise() < amountPaise) throw new ATMException("Insufficient funds. Balance: " + fmt(account.getBalancePaise()), HttpStatus.CONFLICT);
        // Dispense cash via Chain of Responsibility
        Map<Integer, Integer> dispensed = dispenseCash(req.getAmountRupees());
        int rows = accountRepo.debit(accountNumber, amountPaise);
        if (rows == 0) throw new ATMException("Insufficient funds", HttpStatus.CONFLICT);
        String notesStr = formatNotes(dispensed);
        record(accountNumber, mask(account.getCardNumber()), TransactionType.WITHDRAW, amountPaise, TransactionStatus.SUCCESS, notesStr, null);
        log.info("[ATMService] Withdrawal successful | account={} amount=Rs.{} notes={}", accountNumber, req.getAmountRupees(), notesStr);
        return Map.of("withdrawn", fmt(amountPaise), "dispensedNotes", dispensed, "remainingBalance", fmt(accountRepo.findById(accountNumber).get().getBalancePaise()));
    }

    @Transactional
    public Map<String,Object> deposit(String accountNumber, DepositRequest req) {
        log.info("[ATMService] Deposit request | account={} amount=Rs.{}", accountNumber, req.getAmountRupees());
        long amountPaise = req.getAmountRupees() * 100;
        BankAccount account = getAccount(accountNumber);
        accountRepo.credit(accountNumber, amountPaise);
        record(accountNumber, mask(account.getCardNumber()), TransactionType.DEPOSIT, amountPaise, TransactionStatus.SUCCESS, null, null);
        log.info("[ATMService] Deposit successful | account={} amount=Rs.{}", accountNumber, req.getAmountRupees());
        return Map.of("deposited", fmt(amountPaise), "newBalance", fmt(accountRepo.findById(accountNumber).get().getBalancePaise()));
    }

    private Map<Integer,Integer> dispenseCash(long rupees) {
        List<CashCassette> cassettes = cassetteRepo.findAllByOrderByDenominationDesc();
        Map<Integer,Integer> result = new LinkedHashMap<>();
        long remaining = rupees;
        for (CashCassette c : cassettes) {
            if (remaining <= 0) break;
            int notes = (int) Math.min(remaining / c.getDenomination(), c.getNoteCount());
            if (notes > 0) {
                int updated = cassetteRepo.dispense(c.getDenomination(), notes);
                if (updated > 0) { result.put(c.getDenomination(), notes); remaining -= (long) notes * c.getDenomination(); }
            }
        }
        if (remaining > 0) throw new ATMException("ATM cannot dispense exact amount. Available denominations insufficient.", HttpStatus.CONFLICT);
        return result;
    }

    private void record(String acc, String masked, TransactionType type, long paise, TransactionStatus status, String notes, String remarks) {
        ATMTransaction tx = new ATMTransaction(); tx.setAccountNumber(acc); tx.setMaskedCard(masked);
        tx.setType(type); tx.setAmountPaise(paise); tx.setStatus(status); tx.setDispensedNotes(notes); tx.setRemarks(remarks);
        txRepo.save(tx);
    }

    private BankAccount getAccount(String id) { return accountRepo.findById(id).orElseThrow(() -> new ATMException("Account not found: " + id, HttpStatus.NOT_FOUND)); }
    private String mask(String card) { return "**** **** **** " + card.substring(Math.max(0,card.length()-4)); }
    private String fmt(long paise)   { return String.format("Rs.%.2f", paise/100.0); }
    private String formatNotes(Map<Integer,Integer> n) {
        StringBuilder sb = new StringBuilder();
        n.forEach((d,c) -> sb.append(c).append("xRs.").append(d).append(" "));
        return sb.toString().trim();
    }
}
