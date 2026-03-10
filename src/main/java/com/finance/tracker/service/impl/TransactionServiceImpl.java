package com.finance.tracker.service.impl;

import com.finance.tracker.domain.Account;
import com.finance.tracker.domain.Tag;
import com.finance.tracker.domain.Transaction;
import com.finance.tracker.domain.TransactionType;
import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.response.TransactionResponse;
import com.finance.tracker.mapper.TransactionMapper;
import com.finance.tracker.repository.AccountRepository;
import com.finance.tracker.repository.TagRepository;
import com.finance.tracker.repository.TransactionRepository;
import com.finance.tracker.service.TransactionService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private static final String TRANSACTION_NOT_FOUND_MESSAGE = "Transaction not found ";

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TagRepository tagRepository;
    private final TransactionMapper transactionMapper;

    @Override
    public TransactionResponse getTransactionById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, TRANSACTION_NOT_FOUND_MESSAGE + id));
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
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Both startDate and endDate are required for date range filtering");
        }
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be <= endDate");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59, 999_999_999);
        List<Transaction> transactions = transactionRepository.findByOccurredAtBetween(startDateTime, endDateTime);
        return toResponses(transactions);
    }

    @Override
    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        Account account = getAccount(request.getAccountId());
        List<Tag> tags = getTags(request.getTagIds());

        Transaction transaction = transactionMapper.fromRequest(request, account, tags);

        applyDeltaToAccount(account, request.getType(), request.getAmount());
        accountRepository.save(account);
        Transaction saved = transactionRepository.save(transaction);
        return transactionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TransactionResponse updateTransaction(Long id, TransactionRequest request) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, TRANSACTION_NOT_FOUND_MESSAGE + id));

        rollbackDeltaFromAccount(transaction.getAccount(), transaction.getType(), transaction.getAmount());
        accountRepository.save(transaction.getAccount());

        Account account = request.getAccountId() != null
                ? getAccount(request.getAccountId())
                : transaction.getAccount();
        List<Tag> tags = request.getTagIds() != null
                ? getTags(request.getTagIds())
                : transaction.getTags();
        LocalDateTime occurredAt = request.getOccurredAt() != null
                ? request.getOccurredAt()
                : transaction.getOccurredAt();
        BigDecimal amount = request.getAmount() != null ? request.getAmount() : transaction.getAmount();
        String description = request.getDescription() != null ? request.getDescription() : transaction.getDescription();
        TransactionType type = request.getType() != null ? request.getType() : transaction.getType();

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
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, TRANSACTION_NOT_FOUND_MESSAGE + id));

        rollbackDeltaFromAccount(transaction.getAccount(), transaction.getType(), transaction.getAmount());
        accountRepository.save(transaction.getAccount());
        transactionRepository.delete(transaction);
    }

    private Account getAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found: " + accountId));
    }

    private List<Tag> getTags(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }

        List<Tag> tags = tagRepository.findAllById(tagIds);
        if (tags.size() != tagIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Some tags not found");
        }
        return tags;
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

    private List<TransactionResponse> toResponses(List<Transaction> transactions) {
        return transactions.stream()
                .map(transactionMapper::toResponse)
                .toList();
    }
}
