package com.finance.tracker.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.finance.tracker.domain.AsyncTask;
import com.finance.tracker.domain.AsyncTaskStatus;
import com.finance.tracker.domain.TransactionType;
import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.service.TransactionService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class AsyncTransactionExecutorServiceTest {

    @Mock
    private AsyncTaskStorage asyncTaskStorage;

    @Mock
    private TransactionService transactionService;

    @Mock
    private PlatformTransactionManager transactionManager;

    private AsyncTransactionExecutorService service;

    @BeforeEach
    void setUp() {
        service = new AsyncTransactionExecutorService(
                asyncTaskStorage,
                transactionService,
                transactionManager);
    }

    @Test
    void executeTransactionsCreationShouldReturnCompletedFutureWhenTaskIsMissing() {
        CompletableFuture<Void> result =
                service.executeTransactionsCreation("missing", List.of(request("Coffee")), false);

        assertTrue(result.isDone());
        verify(asyncTaskStorage).getTask("missing");
        verifyNoInteractions(transactionService, transactionManager);
    }

    @Test
    void executeTransactionsCreationShouldCompleteEmptyNonTransactionalImport() {
        AsyncTask task = task("task-1");
        when(asyncTaskStorage.getTask("task-1")).thenReturn(task);

        CompletableFuture<Void> result = service.executeTransactionsCreation("task-1", List.of(), false);

        assertTrue(result.isDone());
        assertEquals(AsyncTaskStatus.COMPLETED, task.getStatus());
        assertEquals(100, task.getProgress());
        assertEquals("Created 0 transactions", task.getResult());
        assertNotNull(task.getEndTime());
        verifyNoInteractions(transactionService, transactionManager);
    }

    @Test
    void executeTransactionsCreationShouldCommitEmptyTransactionalImport() {
        AsyncTask task = task("task-1");
        when(asyncTaskStorage.getTask("task-1")).thenReturn(task);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());

        CompletableFuture<Void> result = service.executeTransactionsCreation("task-1", List.of(), true);

        assertTrue(result.isDone());
        assertEquals(AsyncTaskStatus.COMPLETED, task.getStatus());
        assertEquals("Created 0 transactions", task.getResult());
        verify(transactionManager).getTransaction(any());
        verify(transactionManager).commit(any());
        verify(transactionManager, never()).rollback(any());
        verifyNoInteractions(transactionService);
    }

    @Test
    void executeTransactionsCreationShouldCommitTransactionalImportAfterProcessingRequests() {
        AsyncTask task = task("task-1");
        when(asyncTaskStorage.getTask("task-1")).thenReturn(task);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());

        CompletableFuture<Void> result =
                service.executeTransactionsCreation("task-1", List.of(request("Coffee")), true);

        assertTrue(result.isDone());
        assertEquals(AsyncTaskStatus.COMPLETED, task.getStatus());
        assertEquals(100, task.getProgress());
        assertEquals("Created 1 transactions", task.getResult());
        assertNotNull(task.getEndTime());
        verify(transactionService).createTransaction(any(TransactionRequest.class));
        verify(transactionManager).commit(any());
        verify(transactionManager, never()).rollback(any());
    }

    @Test
    void executeTransactionsCreationShouldRollbackTransactionalImportOnFailure() {
        AsyncTask task = task("task-1");
        when(asyncTaskStorage.getTask("task-1")).thenReturn(task);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(transactionService.createTransaction(any(TransactionRequest.class)))
                .thenThrow(new IllegalStateException("boom"));

        CompletableFuture<Void> result =
                service.executeTransactionsCreation("task-1", List.of(request("Coffee")), true);

        assertTrue(result.isDone());
        assertEquals(AsyncTaskStatus.FAILED, task.getStatus());
        assertEquals("Error: boom", task.getResult());
        assertNotNull(task.getEndTime());
        verify(transactionManager).rollback(any());
        verify(transactionManager, never()).commit(any());
    }

    @Test
    void executeTransactionsCreationShouldMarkTaskFailedWhenThreadIsInterrupted() {
        AsyncTask task = task("task-1");
        when(asyncTaskStorage.getTask("task-1")).thenReturn(task);

        try {
            Thread.currentThread().interrupt();

            CompletableFuture<Void> result =
                    service.executeTransactionsCreation("task-1", List.of(request("Coffee")), false);

            assertTrue(result.isDone());
            assertEquals(AsyncTaskStatus.FAILED, task.getStatus());
            assertEquals("Error: Async transaction import interrupted", task.getResult());
            assertNotNull(task.getEndTime());
            assertTrue(Thread.currentThread().isInterrupted());
            verify(transactionService).createTransaction(any(TransactionRequest.class));
        } finally {
            Thread.interrupted();
        }
    }

    private AsyncTask task(String taskId) {
        return AsyncTask.builder()
                .taskId(taskId)
                .status(AsyncTaskStatus.PENDING)
                .startTime(LocalDateTime.of(2026, 4, 5, 12, 0))
                .progress(0)
                .build();
    }

    private TransactionRequest request(String description) {
        return new TransactionRequest(
                LocalDateTime.of(2026, 4, 5, 12, 0),
                new BigDecimal("10.00"),
                description,
                TransactionType.EXPENSE,
                1L,
                List.of());
    }
}
