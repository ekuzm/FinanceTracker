package com.finance.tracker.service;

import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.request.TransactionUpdateRequest;
import com.finance.tracker.dto.response.TransactionResponse;

import java.time.LocalDate;
import java.util.List;

public interface TransactionService {

    TransactionResponse getTransactionById(Long id);

    List<TransactionResponse> getAllTransactions(boolean withEntityGraph);

    List<TransactionResponse> getTransactionsByDateRange(LocalDate startDate, LocalDate endDate);

    TransactionResponse createTransaction(TransactionRequest request);

    List<TransactionResponse> createTransactionsBulkTx(List<TransactionRequest> requests);

    List<TransactionResponse> createTransactionsBulkNoTx(List<TransactionRequest> requests);

    TransactionResponse updateTransaction(Long id, TransactionUpdateRequest request);

    void deleteTransaction(Long id);
}
