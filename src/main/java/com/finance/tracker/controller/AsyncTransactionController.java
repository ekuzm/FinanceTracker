package com.finance.tracker.controller;

import com.finance.tracker.domain.AsyncTask;
import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.service.AsyncTransactionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions/async")
@RequiredArgsConstructor
public class AsyncTransactionController {

    private final AsyncTransactionService asyncTransactionService;

    @PostMapping
    public ResponseEntity<Map<String, String>> createTransactionsAsync(
            @Valid @RequestBody List<@Valid TransactionRequest> requests,
            @RequestParam(defaultValue = "true") boolean transactional) {
        String taskId = asyncTransactionService.createTransactionsAsync(requests, transactional);
        return ResponseEntity.accepted().body(Map.of("taskId", taskId));
    }

    @GetMapping("/status/{taskId}")
    public ResponseEntity<AsyncTask> getTaskStatus(@PathVariable String taskId) {
        AsyncTask task = asyncTransactionService.getTransactionTaskStatus(taskId);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(task);
    }

    @GetMapping("/tasks")
    public ResponseEntity<Map<String, AsyncTask>> getAllAsyncTasks() {
        return ResponseEntity.ok(asyncTransactionService.getAllAsyncTasks());
    }
}
