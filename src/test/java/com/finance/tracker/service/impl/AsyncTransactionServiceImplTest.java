package com.finance.tracker.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.finance.tracker.domain.AsyncTask;
import com.finance.tracker.domain.AsyncTaskStatus;
import com.finance.tracker.domain.TransactionType;
import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.exception.BadRequestException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AsyncTransactionServiceImplTest {

    @Mock
    private AsyncTaskStorage asyncTaskStorage;

    @Mock
    private AsyncTransactionExecutorService asyncTransactionExecutor;

    private AsyncTransactionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AsyncTransactionServiceImpl(asyncTaskStorage, asyncTransactionExecutor);
    }

    @Test
    void createTransactionsAsyncShouldSavePendingTaskAndStartExecution() {
        List<TransactionRequest> requests = List.of(request("Coffee"));

        String taskId = service.createTransactionsAsync(requests, true);

        ArgumentCaptor<AsyncTask> taskCaptor = ArgumentCaptor.forClass(AsyncTask.class);
        verify(asyncTaskStorage).saveTask(taskCaptor.capture());
        AsyncTask savedTask = taskCaptor.getValue();

        assertEquals(taskId, savedTask.getTaskId());
        assertEquals(AsyncTaskStatus.PENDING, savedTask.getStatus());
        assertNotNull(savedTask.getStartTime());
        assertEquals(0, savedTask.getProgress());
        verify(asyncTransactionExecutor).executeTransactionsCreation(taskId, requests, true);
    }

    @Test
    void createTransactionsAsyncShouldRejectNullRequests() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.createTransactionsAsync(null, false));

        assertTrue(exception.getMessage().contains("at least one item"));
        verifyNoInteractions(asyncTaskStorage, asyncTransactionExecutor);
    }

    @Test
    void createTransactionsAsyncShouldRejectEmptyRequests() {
        List<TransactionRequest> requests = List.of();

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.createTransactionsAsync(requests, false));

        assertTrue(exception.getMessage().contains("at least one item"));
        verifyNoInteractions(asyncTaskStorage, asyncTransactionExecutor);
    }

    @Test
    void getTransactionTaskStatusShouldReturnValueFromStorage() {
        AsyncTask task = AsyncTask.builder().taskId("task-1").status(AsyncTaskStatus.COMPLETED).build();
        when(asyncTaskStorage.getTask("task-1")).thenReturn(task);

        AsyncTask result = service.getTransactionTaskStatus("task-1");

        assertSame(task, result);
    }

    @Test
    void getAllAsyncTasksShouldReturnStorageSnapshot() {
        Map<String, AsyncTask> tasks = Map.of(
                "task-1",
                AsyncTask.builder().taskId("task-1").status(AsyncTaskStatus.PENDING).build());
        when(asyncTaskStorage.getAllTasks()).thenReturn(tasks);

        Map<String, AsyncTask> result = service.getAllAsyncTasks();

        assertSame(tasks, result);
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
