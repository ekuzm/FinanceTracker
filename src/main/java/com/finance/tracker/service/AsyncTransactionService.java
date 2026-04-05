package com.finance.tracker.service;

import com.finance.tracker.domain.AsyncTask;
import com.finance.tracker.dto.request.TransactionRequest;
import java.util.List;
import java.util.Map;

public interface AsyncTransactionService {

    String createTransactionsAsync(List<TransactionRequest> requests, boolean transactional);

    AsyncTask getTransactionTaskStatus(String taskId);

    Map<String, AsyncTask> getAllAsyncTasks();
}
