package com.finance.tracker.service.impl;

import com.finance.tracker.domain.Account;
import com.finance.tracker.domain.Tag;
import com.finance.tracker.domain.Transaction;
import com.finance.tracker.domain.TransactionType;
import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.request.TransactionUpdateRequest;
import com.finance.tracker.dto.response.TransactionResponse;
import com.finance.tracker.exception.BadRequestException;
import com.finance.tracker.exception.ResourceNotFoundException;
import com.finance.tracker.mapper.TransactionMapper;
import com.finance.tracker.repository.AccountRepository;
import com.finance.tracker.repository.TagRepository;
import com.finance.tracker.repository.TransactionRepository;
import com.finance.tracker.service.TransactionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private static final String TRANSACTION_NOT_FOUND_MESSAGE = "Transaction not found ";
    private static final String BULK_REQUEST_EMPTY_MESSAGE =
            "Bulk transaction request must contain at least one item";

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TagRepository tagRepository;
    private final TransactionMapper transactionMapper;

    @Override
    public TransactionResponse getTransactionById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(TRANSACTION_NOT_FOUND_MESSAGE + id));
        return transactionMapper.toResponse(transaction);
    }

    @Override
    public List<TransactionResponse> getAllTransactions(boolean withEntityGraph) {
        List<Transaction> transactions;
        if (withEntityGraph) {
            transactions = transactionRepository.findAllTransactionsWithEntityGraph();
        } else {
            transactions = transactionRepository.findAllTransactions();
        }
        return toResponses(transactions);
    }

    @Override
    public List<TransactionResponse> getTransactionsByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BadRequestException("Both startDate and endDate are required for date range filtering");
        }
        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("startDate must be <= endDate");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59, 999_999_999);
        List<Transaction> transactions = transactionRepository.findByOccurredAtBetween(startDateTime, endDateTime);
        return toResponses(transactions);
    }

    @Override
    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        return transactionMapper.toResponse(createTransactionEntity(request));
    }

    @Override
    @Transactional
    public List<TransactionResponse> createTransactionsBulkTx(List<TransactionRequest> requests) {
        return createTransactionsBulk(requests);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<TransactionResponse> createTransactionsBulkNoTx(List<TransactionRequest> requests) {
        return createTransactionsBulk(requests);
    }

    @Override
    @Transactional
    public TransactionResponse updateTransaction(Long id, TransactionUpdateRequest request) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(TRANSACTION_NOT_FOUND_MESSAGE + id));

        rollbackDeltaFromAccount(transaction.getAccount(), transaction.getType(), transaction.getAmount());
        accountRepository.save(transaction.getAccount());

        Account account = Optional.ofNullable(request.getAccountId())
                .map(this::getAccount)
                .orElse(transaction.getAccount());
        List<Tag> tags = Optional.ofNullable(request.getTagIds())
                .map(this::getTags)
                .orElse(transaction.getTags());
        LocalDateTime occurredAt = Optional.ofNullable(request.getOccurredAt())
                .orElse(transaction.getOccurredAt());
        BigDecimal amount = Optional.ofNullable(request.getAmount())
                .orElse(transaction.getAmount());
        String description = Optional.ofNullable(request.getDescription())
                .orElse(transaction.getDescription());
        TransactionType type = Optional.ofNullable(request.getType())
                .orElse(transaction.getType());

        transaction.setAccount(account);
        transaction.setTags(tags);
        transaction.setOccurredAt(occurredAt);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setType(type);

        applyDeltaToAccount(account, type, amount);
        accountRepository.save(account);

        Transaction saved = transactionRepository.save(transaction);
        return transactionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteTransaction(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(TRANSACTION_NOT_FOUND_MESSAGE + id));

        rollbackDeltaFromAccount(transaction.getAccount(), transaction.getType(), transaction.getAmount());
        accountRepository.save(transaction.getAccount());
        transactionRepository.delete(transaction);
    }

    private Account getAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));
    }

    private List<Tag> getTags(List<Long> tagIds) {
        return Optional.ofNullable(tagIds)
                .filter(ids -> !ids.isEmpty())
                .map(ids -> {
                    List<Tag> tags = tagRepository.findAllById(ids);
                    if (tags.size() != ids.size()) {
                        throw new ResourceNotFoundException("Some tags not found");
                    }
                    return tags;
                })
                .orElse(List.of());
    }

    private void applyDeltaToAccount(Account account, TransactionType type, BigDecimal amount) {
        BigDecimal delta = signedAmount(type, amount);
        account.setBalance(account.getBalance().add(delta));
    }

    private void rollbackDeltaFromAccount(Account account, TransactionType type, BigDecimal amount) {
        BigDecimal delta = signedAmount(type, amount);
        account.setBalance(account.getBalance().subtract(delta));
    }

    private BigDecimal signedAmount(TransactionType type, BigDecimal amount) {
        if (type == TransactionType.INCOME) {
            return amount;
        }
        return amount.negate();
    }

    private Transaction createTransactionEntity(TransactionRequest request) {
        Account account = getAccount(request.getAccountId());
        List<Tag> tags = getTags(request.getTagIds());
        Transaction transaction = transactionMapper.fromRequest(request, account, tags);

        applyDeltaToAccount(account, request.getType(), request.getAmount());
        accountRepository.save(account);
        return transactionRepository.save(transaction);
    }

    private List<TransactionResponse> createTransactionsBulk(List<TransactionRequest> requests) {
        List<TransactionRequest> bulkRequests = Optional.ofNullable(requests)
                .filter(items -> !items.isEmpty())
                .orElseThrow(() -> new BadRequestException(BULK_REQUEST_EMPTY_MESSAGE));

        return bulkRequests.stream()
                .map(this::createTransactionEntity)
                .map(transactionMapper::toResponse)
                .toList();
    }

    private List<TransactionResponse> toResponses(List<Transaction> transactions) {
        return transactions.stream()
                .map(transactionMapper::toResponse)
                .toList();
    }
}
