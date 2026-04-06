package com.finance.tracker.service.impl;

import com.finance.tracker.domain.AsyncTask;
import com.finance.tracker.domain.AsyncTaskStatus;
import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.exception.LoggingException;
import com.finance.tracker.service.TransactionService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class AsyncTransactionExecutorService {

    private final AsyncTaskStorage asyncTaskStorage;
    private final TransactionService transactionService;
    private final PlatformTransactionManager transactionManager;

    @Async
    public CompletableFuture<Void> executeTransactionsCreation(
            String taskId,
            List<TransactionRequest> requests,
            boolean transactional) {
        AsyncTask task = asyncTaskStorage.getTask(taskId);

        if (task == null) {
            return CompletableFuture.completedFuture(null);
        }

        task.setStatus(AsyncTaskStatus.IN_PROGRESS);
        try {
            Thread.sleep(10000);
            if (transactional) {
                executeTransactionalImport(task, requests);
            } else {
                executeNonTransactionalImport(task, requests);
            }

            task.setStatus(AsyncTaskStatus.COMPLETED);
            task.setEndTime(LocalDateTime.now());
            task.setProgress(100);
            task.setResult("Created " + requests.size() + " transactions");
        } catch (Exception exception) {
            task.setStatus(AsyncTaskStatus.FAILED);
            task.setEndTime(LocalDateTime.now());
            task.setResult("Error: " + exception.getMessage());
        }

        return CompletableFuture.completedFuture(null);
    }

    private void executeTransactionalImport(AsyncTask task, List<TransactionRequest> requests) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status -> {
            int total = requests.size();
            for (int i = 0; i < total; i++) {
                transactionService.createTransaction(requests.get(i));
                task.setProgress((i + 1) * 100 / total);
                sleepWithInterruptionHandling(task);
            }
        });
    }

    private void executeNonTransactionalImport(AsyncTask task, List<TransactionRequest> requests) {
        int total = requests.size();
        for (int i = 0; i < total; i++) {
            transactionService.createTransaction(requests.get(i));
            task.setProgress((i + 1) * 100 / total);
            sleepWithInterruptionHandling(task);
        }
    }

    private void sleepWithInterruptionHandling(AsyncTask task) {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            task.setStatus(AsyncTaskStatus.FAILED);
            task.setEndTime(LocalDateTime.now());
            task.setResult("Task was interrupted while waiting");
            throw new LoggingException("Async transaction import interrupted", exception);
        }
    }
}
