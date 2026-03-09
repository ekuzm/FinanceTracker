package com.finance.tracker.controller;

import com.finance.tracker.dto.request.AccountTransferRequest;
import com.finance.tracker.dto.request.AccountRequest;
import com.finance.tracker.dto.response.AccountResponse;
import com.finance.tracker.service.AccountService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/api/v1/accounts/{id}")
    public ResponseEntity<AccountResponse> getAccountById(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getAccountById(id));
    }

    @GetMapping("/api/v1/accounts")
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @PostMapping("/api/v1/accounts")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody AccountRequest request) {
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/api/v1/account/transfer")
    public ResponseEntity<Void> createTransfer(
            @Valid @RequestBody AccountTransferRequest request,
            @RequestParam(defaultValue = "true") boolean transactional,
            @RequestParam(defaultValue = "false") boolean failAfterDebit) {
        if (transactional) {
            accountService.createTransferTx(request, failAfterDebit);
        } else {
            accountService.createTransferNoTx(request, failAfterDebit);
        }
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/v1/accounts/{id}")
    public ResponseEntity<AccountResponse> updateAccount(@PathVariable Long id,
            @RequestBody AccountRequest request) {
        return ResponseEntity.ok(accountService.updateAccount(id, request));
    }

    @DeleteMapping("/api/v1/accounts/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }
}
