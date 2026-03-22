package lld.atm.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lld.atm.dto.*;
import lld.atm.service.ATMService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/v1/atm") @RequiredArgsConstructor
@Tag(name = "ATM Machine", description = "Stateless ATM — card validation, PIN auth, withdraw/deposit with cash dispenser chain")
public class ATMController {
    private static final Logger log = LoggerFactory.getLogger(ATMController.class);
    private final ATMService service;

    @PostMapping("/card/validate")
    @Operation(summary = "Step 1: Validate card number")
    public Map<String,Object> validateCard(@Valid @RequestBody InsertCardRequest req) {
        log.info("[ATMController] POST /card/validate | masked={}", req.getCardNumber().substring(12));
        return service.validateCard(req);
    }

    @PostMapping("/accounts/{accountNumber}/pin/verify")
    @Operation(summary = "Step 2: Verify PIN (locked after 3 wrong attempts)")
    public Map<String,Object> verifyPin(@PathVariable String accountNumber, @Valid @RequestBody EnterPinRequest req) {
        return service.verifyPin(accountNumber, req);
    }

    @GetMapping("/accounts/{accountNumber}/balance")
    @Operation(summary = "Step 3a: Check balance")
    public Map<String,Object> checkBalance(@PathVariable String accountNumber) { return service.checkBalance(accountNumber); }

    @PostMapping("/accounts/{accountNumber}/withdraw")
    @Operation(summary = "Step 3b: Withdraw cash", description = "Dispenses using Rs.2000 → Rs.500 → Rs.200 → Rs.100 chain")
    public Map<String,Object> withdraw(@PathVariable String accountNumber, @Valid @RequestBody WithdrawRequest req) {
        log.info("[ATMController] POST /{}/withdraw | amount=Rs.{}", accountNumber, req.getAmountRupees());
        return service.withdraw(accountNumber, req);
    }

    @PostMapping("/accounts/{accountNumber}/deposit")
    @Operation(summary = "Step 3c: Deposit cash")
    public Map<String,Object> deposit(@PathVariable String accountNumber, @Valid @RequestBody DepositRequest req) { return service.deposit(accountNumber, req); }
}
