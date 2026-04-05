package com.finance.tracker.service.impl;

import com.finance.tracker.domain.AsyncTask;
import com.finance.tracker.domain.AsyncTaskStatus;
import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.exception.BadRequestException;
import com.finance.tracker.service.AsyncTransactionService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AsyncTransactionServiceImpl implements AsyncTransactionService {

    private static final String BULK_REQUEST_EMPTY_MESSAGE =
            "Bulk transaction request must contain at least one item";

    private final AsyncTaskStorage asyncTaskStorage;
    private final AsyncTransactionExecutorService asyncTransactionExecutor;

    @Override
    public String createTransactionsAsync(List<TransactionRequest> requests, boolean transactional) {
        List<TransactionRequest> bulkRequests = Optional.ofNullable(requests)
                .filter(items -> !items.isEmpty())
                .orElseThrow(() -> new BadRequestException(BULK_REQUEST_EMPTY_MESSAGE));

        String taskId = UUID.randomUUID().toString();

        AsyncTask task = AsyncTask.builder()
                .taskId(taskId)
                .status(AsyncTaskStatus.PENDING)
                .startTime(LocalDateTime.now())
                .progress(0)
                .build();

        asyncTaskStorage.saveTask(task);
        asyncTransactionExecutor.executeTransactionsCreation(taskId, bulkRequests, transactional);

        return taskId;
    }

    @Override
    public AsyncTask getTransactionTaskStatus(String taskId) {
        return asyncTaskStorage.getTask(taskId);
    }

    @Override
    public Map<String, AsyncTask> getAllAsyncTasks() {
        return asyncTaskStorage.getAllTasks();
    }
}
