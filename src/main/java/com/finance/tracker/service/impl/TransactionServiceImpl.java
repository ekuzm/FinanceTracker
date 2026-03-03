package com.finance.tracker.service.impl;

import com.finance.tracker.domain.Account;
import com.finance.tracker.domain.Budget;
import com.finance.tracker.domain.Tag;
import com.finance.tracker.domain.Transaction;
import com.finance.tracker.domain.User;
import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.response.TransactionResponse;
import com.finance.tracker.mapper.TransactionMapper;
import com.finance.tracker.repository.AccountRepository;
import com.finance.tracker.repository.BudgetRepository;
import com.finance.tracker.repository.TagRepository;
import com.finance.tracker.repository.TransactionRepository;
import com.finance.tracker.repository.UserRepository;
import com.finance.tracker.service.TransactionService;
import com.finance.tracker.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final BudgetRepository budgetRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final TransactionMapper transactionMapper;

    @Override
    public TransactionResponse getTransactionById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found " + id));
        return transactionMapper.toResponse(transaction, true, true);
    }

    @Override
    public List<TransactionResponse> getAllTransactions(boolean withEntityGraph, boolean includeTransfers) {
        List<Transaction> transactions = withEntityGraph
                ? (includeTransfers
                        ? transactionRepository.findAllTransactionsWithEntityGraph()
                        : transactionRepository.findAllTransactionsWithoutTransfersWithEntityGraph())
                : (includeTransfers
                        ? transactionRepository.findAllTransactions()
                        : transactionRepository.findAllTransactionsWithoutTransfers());
        return toResponses(transactions);
    }

    @Override
    public List<TransactionResponse> getTransactionsByDateRange(
            LocalDate startDate, LocalDate endDate, boolean includeTransfers) {
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
        List<Transaction> transactions = includeTransfers
                ? transactionRepository.findByOccurredAtBetween(startDateTime, endDateTime)
                : transactionRepository.findByOccurredAtBetweenAndTransferIdIsNull(startDateTime, endDateTime);
        return toResponses(transactions);
    }

    @Override
    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        User user = getUser(request.getUserId());
        Account account = getAccount(request.getAccountId());
        Budget budget = getBudget(request.getBudgetId());
        List<Tag> tags = getTagsForUser(request.getTagIds(), user.getId());

        validateTransactionIntegrity(user, account, budget, tags, request.getOccurredAt(), null);
        Transaction transaction = transactionMapper.fromRequest(request, user, account, budget, tags);

        applyDeltaToAccount(account, request.getType(), request.getAmount());
        accountRepository.save(account);
        Transaction saved = transactionRepository.save(transaction);
        return transactionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TransactionResponse updateTransaction(Long id, TransactionRequest request) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found " + id));

        rollbackDeltaFromAccount(transaction.getAccount(), transaction.getType(), transaction.getAmount());
        accountRepository.save(transaction.getAccount());

        User user = request.getUserId() != null ? getUser(request.getUserId()) : transaction.getUser();
        Account account = request.getAccountId() != null
                ? getAccount(request.getAccountId())
                : transaction.getAccount();
        Budget budget = request.getBudgetId() != null ? getBudget(request.getBudgetId()) : transaction.getBudget();
        List<Tag> tags = request.getTagIds() != null
                ? getTagsForUser(request.getTagIds(), user.getId())
                : transaction.getTags();
        LocalDateTime occurredAt = request.getOccurredAt() != null
                ? request.getOccurredAt()
                : transaction.getOccurredAt();
        BigDecimal amount = request.getAmount() != null ? request.getAmount() : transaction.getAmount();
        String description = request.getDescription() != null ? request.getDescription() : transaction.getDescription();
        TransactionType type = request.getType() != null ? request.getType() : transaction.getType();

        validateTransactionIntegrity(user, account, budget, tags, occurredAt, transaction.getTransferId());

        transaction.setUser(user);
        transaction.setAccount(account);
        transaction.setBudget(budget);
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found " + id));

        rollbackDeltaFromAccount(transaction.getAccount(), transaction.getType(), transaction.getAmount());
        accountRepository.save(transaction.getAccount());
        transactionRepository.delete(transaction);
    }

    private Budget getBudget(Long budgetId) {
        return budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found: " + budgetId));
    }

    private Account getAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found: " + accountId));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId));
    }

    private List<Tag> getTagsForUser(List<Long> tagIds, Long userId) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }

        List<Tag> tags = tagRepository.findAllById(tagIds);
        if (tags.size() != tagIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Some tags not found");
        }

        boolean foreignTagExists = tags.stream()
                .anyMatch(tag -> tag.getUser() == null || !userId.equals(tag.getUser().getId()));
        if (foreignTagExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Some tags do not belong to provided user");
        }
        return tags;
    }

    private void validateTransactionIntegrity(
            User user,
            Account account,
            Budget budget,
            List<Tag> tags,
            LocalDateTime occurredAt,
            java.util.UUID transferId) {
        Long userId = user.getId();
        if (account.getUser() == null || !userId.equals(account.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account does not belong to provided user");
        }

        if (transferId == null && budget == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Budget is required for non-transfer transaction");
        }
        if (transferId != null && budget != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Transfer transactions cannot have budget assigned");
        }

        if (budget != null) {
            if (budget.getUser() == null || !userId.equals(budget.getUser().getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Budget does not belong to provided user");
            }
            LocalDate occurredDate = occurredAt.toLocalDate();
            if (occurredDate.isBefore(budget.getStartDate()) || occurredDate.isAfter(budget.getEndDate())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Transaction date is outside budget period");
            }
        }

        if (transferId != null && tags != null && !tags.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Transfer transactions cannot have tags");
        }
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
                .map(transaction -> transactionMapper.toResponse(transaction, true, true))
                .toList();
    }
}
