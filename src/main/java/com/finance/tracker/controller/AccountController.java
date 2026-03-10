package com.finance.tracker.controller;

import com.finance.tracker.controller.api.AccountControllerApi;
import com.finance.tracker.dto.request.AccountTransferRequest;
import com.finance.tracker.dto.request.AccountRequest;
import com.finance.tracker.dto.request.AccountUpdateRequest;
import com.finance.tracker.dto.response.AccountResponse;
import com.finance.tracker.service.AccountService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AccountController implements AccountControllerApi {

    private final AccountService accountService;

    public ResponseEntity<AccountResponse> getAccountById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(accountService.getAccountById(id));
    }

    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody AccountRequest request) {
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

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

    public ResponseEntity<AccountResponse> updateAccount(@PathVariable("id") Long id,
            @Valid @RequestBody AccountUpdateRequest request) {
        return ResponseEntity.ok(accountService.updateAccount(id, request));
    }

    public ResponseEntity<Void> deleteAccount(@PathVariable("id") Long id) {
        accountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }
}
