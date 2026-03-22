package lld.splitwise.service;
import jakarta.transaction.Transactional;
import lld.splitwise.dto.*;
import lld.splitwise.entity.*;
import lld.splitwise.exception.SplitwiseException;
import lld.splitwise.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class SplitwiseService {
    private static final Logger log = LoggerFactory.getLogger(SplitwiseService.class);
    private final UserRepository     userRepo;
    private final ExpenseRepository  expenseRepo;
    private final BalanceRepository  balanceRepo;
    private final GroupRepository    groupRepo;
    private final SplitCalculator    calculator;

    @Transactional
    public AppUser createUser(CreateUserRequest req) {
        log.info("[SplitwiseService] Creating user | id={} email={}", req.getUserId(), req.getEmail());
        if (userRepo.existsById(req.getUserId()))
            throw new SplitwiseException("User already exists: " + req.getUserId(), HttpStatus.CONFLICT);
        if (userRepo.existsByEmail(req.getEmail()))
            throw new SplitwiseException("Email already registered: " + req.getEmail(), HttpStatus.CONFLICT);
        AppUser user = new AppUser();
        user.setUserId(req.getUserId()); user.setName(req.getName());
        user.setEmail(req.getEmail());   user.setPhone(req.getPhone());
        userRepo.save(user);
        log.info("[SplitwiseService] User created | id={}", req.getUserId());
        return user;
    }

    @Transactional
    public Expense addExpense(AddExpenseRequest req) {
        log.info("[SplitwiseService] Adding expense | desc='{}' amount=Rs.{} paidBy={} split={}",
            req.getDescription(), req.getAmountRupees(), req.getPaidByUserId(), req.getSplitType());
        validateUsers(req.getParticipantIds());
        validateUsers(List.of(req.getPaidByUserId()));
        List<String> participants = req.getParticipantIds();
        if (!participants.contains(req.getPaidByUserId())) {
            participants = new ArrayList<>(participants);
            participants.add(req.getPaidByUserId());
        }
        Map<String, Long> splits = calculator.calculate(req);
        Expense expense = new Expense();
        expense.setDescription(req.getDescription());
        expense.setTotalPaise(Math.round(req.getAmountRupees() * 100));
        expense.setPaidByUserId(req.getPaidByUserId());
        expense.setSplitType(req.getSplitType());
        expense.setCategory(req.getCategory() != null ? req.getCategory() : lld.splitwise.enums.ExpenseCategory.OTHER);
        expense.setGroupId(req.getGroupId());
        List<ExpenseSplit> splitEntities = new ArrayList<>();
        splits.forEach((uid, paise) -> {
            ExpenseSplit s = new ExpenseSplit();
            s.setExpense(expense); s.setUserId(uid); s.setAmountPaise(paise);
            splitEntities.add(s);
        });
        expense.setSplits(splitEntities);
        expenseRepo.save(expense);
        // Update balances: everyone except payer owes the payer
        splits.forEach((uid, paise) -> {
            if (!uid.equals(req.getPaidByUserId())) {
                adjustBalance(uid, req.getPaidByUserId(), paise);
                log.debug("[SplitwiseService] Balance updated | {} owes {} Rs.{}", uid, req.getPaidByUserId(), paise/100.0);
            }
        });
        log.info("[SplitwiseService] Expense added | id={} splits={}", expense.getId(), splits.size());
        return expense;
    }

    @Transactional
    public void settle(SettleRequest req) {
        log.info("[SplitwiseService] Settlement | from={} to={} amount=Rs.{}",
            req.getPayerUserId(), req.getReceiverUserId(), req.getAmountRupees());
        validateUsers(List.of(req.getPayerUserId(), req.getReceiverUserId()));
        long paise = Math.round(req.getAmountRupees() * 100);
        adjustBalance(req.getPayerUserId(), req.getReceiverUserId(), -paise);
        log.info("[SplitwiseService] Settlement recorded | {} paid {} Rs.{}", req.getPayerUserId(), req.getReceiverUserId(), req.getAmountRupees());
    }

    public BalanceResponse getBalance(String userId) {
        log.debug("[SplitwiseService] Fetching balances for user={}", userId);
        AppUser user = getUser(userId);
        List<Balance> balances = balanceRepo.findByDebtorIdOrCreditorId(userId, userId);
        List<BalanceResponse.BalanceEntry> owes = new ArrayList<>();
        List<BalanceResponse.BalanceEntry> owedBy = new ArrayList<>();
        long net = 0;
        for (Balance b : balances) {
            if (b.getAmountPaise() <= 0) continue;
            if (b.getDebtorId().equals(userId)) {
                AppUser creditor = getUser(b.getCreditorId());
                owes.add(BalanceResponse.BalanceEntry.builder().userId(b.getCreditorId()).userName(creditor.getName()).amount(fmt(b.getAmountPaise())).build());
                net -= b.getAmountPaise();
            } else {
                AppUser debtor = getUser(b.getDebtorId());
                owedBy.add(BalanceResponse.BalanceEntry.builder().userId(b.getDebtorId()).userName(debtor.getName()).amount(fmt(b.getAmountPaise())).build());
                net += b.getAmountPaise();
            }
        }
        return BalanceResponse.builder().userId(userId).userName(user.getName()).owes(owes).owedBy(owedBy)
            .netBalance((net >= 0 ? "+" : "") + fmt(net)).build();
    }

    public SettlementSuggestion getSimplifiedSettlements() {
        log.debug("[SplitwiseService] Computing simplified settlements");
        List<Balance> all = balanceRepo.findAll();
        Map<String, Long> net = new HashMap<>();
        for (Balance b : all) {
            if (b.getAmountPaise() <= 0) continue;
            net.merge(b.getCreditorId(), b.getAmountPaise(), Long::sum);
            net.merge(b.getDebtorId(),  -b.getAmountPaise(), Long::sum);
        }
        PriorityQueue<Map.Entry<String,Long>> creditors = new PriorityQueue<>((a,b2) -> Long.compare(b2.getValue(), a.getValue()));
        PriorityQueue<Map.Entry<String,Long>> debtors   = new PriorityQueue<>((a,b2) -> Long.compare(a.getValue(), b2.getValue()));
        net.entrySet().forEach(e -> { if (e.getValue() > 0) creditors.add(e); else if (e.getValue() < 0) debtors.add(e); });
        List<SettlementSuggestion.Transaction> txns = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            var cr = creditors.poll(); var dr = debtors.poll();
            long amt = Math.min(cr.getValue(), -dr.getValue());
            AppUser from = userRepo.findById(dr.getKey()).orElse(null);
            AppUser to   = userRepo.findById(cr.getKey()).orElse(null);
            txns.add(SettlementSuggestion.Transaction.builder()
                .fromUserId(dr.getKey()).fromUserName(from != null ? from.getName() : dr.getKey())
                .toUserId(cr.getKey()).toUserName(to != null ? to.getName() : cr.getKey())
                .amount(fmt(amt)).build());
            long crLeft = cr.getValue() - amt; long drLeft = dr.getValue() + amt;
            if (crLeft > 0) creditors.add(Map.entry(cr.getKey(), crLeft));
            if (drLeft < 0) debtors.add(Map.entry(dr.getKey(), drLeft));
        }
        log.info("[SplitwiseService] Simplified settlements: {} transactions", txns.size());
        return SettlementSuggestion.builder().transactions(txns)
            .message(txns.isEmpty() ? "Everyone is settled up!" : txns.size() + " transaction(s) needed").build();
    }

    private void adjustBalance(String debtorId, String creditorId, long deltaPaise) {
        Balance balance = balanceRepo.findByDebtorIdAndCreditorId(debtorId, creditorId).orElseGet(() -> {
            Balance b = new Balance(); b.setDebtorId(debtorId); b.setCreditorId(creditorId); b.setAmountPaise(0L);
            return balanceRepo.save(b);
        });
        balance.setAmountPaise(balance.getAmountPaise() + deltaPaise);
        balanceRepo.save(balance);
    }

    private void validateUsers(List<String> userIds) {
        userIds.forEach(uid -> { if (!userRepo.existsById(uid)) throw new SplitwiseException("User not found: " + uid, HttpStatus.NOT_FOUND); });
    }

    private AppUser getUser(String id) {
        return userRepo.findById(id).orElseThrow(() -> new SplitwiseException("User not found: " + id, HttpStatus.NOT_FOUND));
    }

    private String fmt(long paise) { return String.format("Rs.%.2f", paise / 100.0); }
}
