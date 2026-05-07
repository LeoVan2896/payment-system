package com.payment.backend.controller;

import com.payment.backend.dto.AccountResponse;
import com.payment.backend.dto.TransactionResponse;
import com.payment.backend.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@CrossOrigin
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(
                AccountResponse.from(accountService.getAccountByNumber(accountNumber))
        );
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AccountResponse>> getUserAccounts(@PathVariable Long userId) {
        List<AccountResponse> responses = accountService.getAccountsByUser(userId)
                .stream()
                .map(AccountResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(@RequestBody Map<String, String> request) {
        String sender = request.get("senderAccountNumber");
        String receiver = request.get("receiverAccountNumber");
        BigDecimal amount = new BigDecimal(request.get("amount"));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.transfer(sender, receiver, amount));
    }

    @PostMapping("/user/{userId}")
    public ResponseEntity<AccountResponse> createAccount(@PathVariable Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AccountResponse.from(accountService.createAccount(userId)));
    }

    @PostMapping("/{accountNumber}/deposit")
    public ResponseEntity<AccountResponse> deposit(@PathVariable String accountNumber,
                                                   @RequestBody Map<String, String> request) {
        BigDecimal amount = new BigDecimal(request.get("amount"));
        return ResponseEntity.ok(
                AccountResponse.from(accountService.deposit(accountNumber, amount))
        );
    }
}
