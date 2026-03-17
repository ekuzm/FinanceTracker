package com.finance.tracker.controller;

import com.finance.tracker.controller.api.TransactionControllerApi;
import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.request.TransactionUpdateRequest;
import com.finance.tracker.dto.response.TransactionResponse;
import com.finance.tracker.service.TransactionService;
import jakarta.validation.Valid;
import java.time.LocalDate;
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
public class TransactionController implements TransactionControllerApi {

    private final TransactionService service;

    @GetMapping("/api/v1/transactions/{id}")
    public ResponseEntity<TransactionResponse> getById(@PathVariable("id") final Long id) {
        return ResponseEntity.ok(service.getTransactionById(id));
    }

    @GetMapping("/api/v1/transactions")
    public ResponseEntity<List<TransactionResponse>> getByDateRange(
            @RequestParam(required = false) final LocalDate startDate,
            @RequestParam(required = false) final LocalDate endDate,
            @RequestParam(required = false, defaultValue = "false") final boolean withEntityGraph) {
        if (startDate == null && endDate == null) {
            return ResponseEntity.ok(service.getAllTransactions(withEntityGraph));
        }
        return ResponseEntity.ok(service.getTransactionsByDateRange(startDate, endDate));
    }

    @PostMapping("/api/v1/transactions")
    public ResponseEntity<TransactionResponse> createTransaction(@Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = service.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/api/v1/transactions/bulk")
    public ResponseEntity<List<TransactionResponse>> createTransactionsBulk(
            @Valid @RequestBody List<@Valid TransactionRequest> requests,
            @RequestParam(defaultValue = "true") boolean transactional) {
        List<TransactionResponse> response = transactional
                ? service.createTransactionsBulkTx(requests)
                : service.createTransactionsBulkNoTx(requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/api/v1/transactions/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(@PathVariable("id") Long id,
            @Valid @RequestBody TransactionUpdateRequest request) {
        return ResponseEntity.ok(service.updateTransaction(id, request));
    }

    @DeleteMapping("/api/v1/transactions/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable("id") Long id) {
        service.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }
}
