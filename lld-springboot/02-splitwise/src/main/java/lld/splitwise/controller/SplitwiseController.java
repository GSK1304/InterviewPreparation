package lld.splitwise.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lld.splitwise.dto.*;
import lld.splitwise.entity.*;
import lld.splitwise.service.SplitwiseService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/v1/splitwise") @RequiredArgsConstructor
@Tag(name = "Splitwise", description = "Expense splitting with EQUAL, EXACT, PERCENT, SHARE strategies")
public class SplitwiseController {
    private static final Logger log = LoggerFactory.getLogger(SplitwiseController.class);
    private final SplitwiseService service;

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a user")
    public AppUser createUser(@Valid @RequestBody CreateUserRequest req) {
        log.info("[SplitwiseController] POST /users | userId={}", req.getUserId());
        return service.createUser(req);
    }

    @PostMapping("/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add an expense", description = "Supports EQUAL, EXACT, PERCENT, SHARE split types")
    public Expense addExpense(@Valid @RequestBody AddExpenseRequest req) {
        log.info("[SplitwiseController] POST /expenses | desc='{}' paidBy={} split={}", req.getDescription(), req.getPaidByUserId(), req.getSplitType());
        return service.addExpense(req);
    }

    @PostMapping("/settle")
    @Operation(summary = "Record a settlement payment")
    public ResponseEntity<String> settle(@Valid @RequestBody SettleRequest req) {
        log.info("[SplitwiseController] POST /settle | from={} to={} amount={}", req.getPayerUserId(), req.getReceiverUserId(), req.getAmountRupees());
        service.settle(req);
        return ResponseEntity.ok("Settlement recorded successfully");
    }

    @GetMapping("/balances/{userId}")
    @Operation(summary = "Get balances for a user")
    public BalanceResponse getBalance(@PathVariable String userId) {
        log.debug("[SplitwiseController] GET /balances/{}", userId);
        return service.getBalance(userId);
    }

    @GetMapping("/settlements/simplified")
    @Operation(summary = "Get minimum transactions to settle all debts")
    public SettlementSuggestion getSimplifiedSettlements() {
        log.info("[SplitwiseController] GET /settlements/simplified");
        return service.getSimplifiedSettlements();
    }
}
