package com.finance.tracker.service.impl;

import com.finance.tracker.domain.Account;
import com.finance.tracker.domain.Transaction;
import com.finance.tracker.domain.TransactionType;
import com.finance.tracker.domain.Transfer;
import com.finance.tracker.dto.request.TransferRequest;
import com.finance.tracker.dto.response.TransferResponse;
import com.finance.tracker.mapper.TransferMapper;
import com.finance.tracker.repository.AccountRepository;
import com.finance.tracker.repository.TransactionRepository;
import com.finance.tracker.repository.TransferRepository;
import com.finance.tracker.service.TransferService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private static final String ACCOUNT_NOT_FOUND_MESSAGE = "Account not found ";
    private static final String TRANSFER_NOT_FOUND_MESSAGE = "Transfer not found ";

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransferRepository transferRepository;
    private final TransferMapper transferMapper;

    @Override
    public TransferResponse getTransferById(UUID id) {
        Transfer transfer = getTransfer(id);
        return transferMapper.toResponse(transfer);
    }

    @Override
    public List<TransferResponse> getAllTransfers() {
        return transferRepository.findAll().stream()
                .map(transferMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TransferResponse createTransferTx(TransferRequest request, boolean failAfterDebit) {
        Transfer created = executeCreateTransfer(request, failAfterDebit);
        return transferMapper.toResponse(created);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public TransferResponse createTransferNoTx(TransferRequest request, boolean failAfterDebit) {
        Transfer created = executeCreateTransfer(request, failAfterDebit);
        return transferMapper.toResponse(created);
    }

    @Override
    @Transactional
    public TransferResponse updateTransfer(UUID id, TransferRequest request) {
        Transfer transfer = getTransfer(id);
        TransferPair pair = resolveTransferPair(transfer);

        rollbackTransferImpact(pair.fromAccount(), pair.toAccount(), pair.amount());

        Account fromAccount = request.getFromAccountId() != null
                ? getAccount(request.getFromAccountId())
                : pair.fromAccount();
        Account toAccount = request.getToAccountId() != null
                ? getAccount(request.getToAccountId())
                : pair.toAccount();
        BigDecimal amount = request.getAmount() != null ? request.getAmount() : pair.amount();
        LocalDateTime occurredAt = request.getOccurredAt() != null ? request.getOccurredAt() : pair.occurredAt();
        String note = request.getNote() != null ? normalizeNote(request.getNote()) : transfer.getNote();

        validatePositiveAmount(amount);
        validateAccountsDistinct(fromAccount, toAccount);
        validateTransferOwnership(fromAccount, toAccount);
        validateSufficientFunds(fromAccount, amount);

        applyTransferImpact(fromAccount, toAccount, amount);

        transfer.setNote(note);

        Transaction expense = pair.expense();
        expense.setOccurredAt(occurredAt);
        expense.setAmount(amount);
        expense.setDescription(note + " to account " + toAccount.getId());
        expense.setType(TransactionType.EXPENSE);
        expense.setTransfer(transfer);
        expense.setAccount(fromAccount);
        expense.setTags(List.of());

        Transaction income = pair.income();
        income.setOccurredAt(occurredAt);
        income.setAmount(amount);
        income.setDescription(note + " from account " + fromAccount.getId());
        income.setType(TransactionType.INCOME);
        income.setTransfer(transfer);
        income.setAccount(toAccount);
        income.setTags(List.of());

        Transaction savedExpense = transactionRepository.save(expense);
        Transaction savedIncome = transactionRepository.save(income);
        transfer.setTransactions(new ArrayList<>(List.of(savedExpense, savedIncome)));

        Transfer saved = transferRepository.save(transfer);
        return transferMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteTransfer(UUID id) {
        Transfer transfer = getTransfer(id);
        TransferPair pair = resolveTransferPair(transfer);

        rollbackTransferImpact(pair.fromAccount(), pair.toAccount(), pair.amount());
        transferRepository.delete(transfer);
    }

    private Transfer executeCreateTransfer(TransferRequest request, boolean failAfterDebit) {
        validatePositiveAmount(request.getAmount());

        Account fromAccount = getAccount(request.getFromAccountId());
        Account toAccount = getAccount(request.getToAccountId());

        validateAccountsDistinct(fromAccount, toAccount);
        validateTransferOwnership(fromAccount, toAccount);
        validateSufficientFunds(fromAccount, request.getAmount());

        LocalDateTime occurredAt = request.getOccurredAt() != null ? request.getOccurredAt() : LocalDateTime.now();
        String note = normalizeNote(request.getNote());

        Transfer transfer = new Transfer();
        transfer.setNote(note);
        transfer = transferRepository.save(transfer);

        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        accountRepository.save(fromAccount);

        Transaction expense = transactionRepository.save(buildTransferTransaction(
                occurredAt,
                request.getAmount(),
                note + " to account " + toAccount.getId(),
                TransactionType.EXPENSE,
                transfer,
                fromAccount));

        if (failAfterDebit) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Forced error after debit operation");
        }

        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));
        accountRepository.save(toAccount);

        Transaction income = transactionRepository.save(buildTransferTransaction(
                occurredAt,
                request.getAmount(),
                note + " from account " + fromAccount.getId(),
                TransactionType.INCOME,
                transfer,
                toAccount));

        transfer.setTransactions(new ArrayList<>(List.of(expense, income)));
        return transfer;
    }

    private Account getAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND_MESSAGE + accountId));
    }

    private Transfer getTransfer(UUID transferId) {
        return transferRepository.findById(transferId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        TRANSFER_NOT_FOUND_MESSAGE + transferId));
    }

    private void validateAccountsDistinct(Account fromAccount, Account toAccount) {
        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source and target account must be different");
        }
    }

    private void validateTransferOwnership(Account fromAccount, Account toAccount) {
        Long fromUserId = fromAccount.getUser() != null ? fromAccount.getUser().getId() : null;
        Long toUserId = toAccount.getUser() != null ? toAccount.getUser().getId() : null;
        if (fromUserId == null || !fromUserId.equals(toUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Both accounts must belong to the same user");
        }
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transfer amount must be > 0");
        }
    }

    private void validateSufficientFunds(Account fromAccount, BigDecimal amount) {
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Insufficient funds on source account " + fromAccount.getId());
        }
    }

    private String normalizeNote(String note) {
        if (note == null || note.isBlank()) {
            return "Transfer";
        }
        String normalized = note.trim();
        if (normalized.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transfer note length must be <= 255");
        }
        return normalized;
    }

    private void rollbackTransferImpact(Account fromAccount, Account toAccount, BigDecimal amount) {
        fromAccount.setBalance(fromAccount.getBalance().add(amount));
        toAccount.setBalance(toAccount.getBalance().subtract(amount));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
    }

    private void applyTransferImpact(Account fromAccount, Account toAccount, BigDecimal amount) {
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
    }

    private TransferPair resolveTransferPair(Transfer transfer) {
        Transaction expense = transfer.getTransactions().stream()
                .filter(transaction -> transaction.getType() == TransactionType.EXPENSE)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Transfer " + transfer.getId() + " has no EXPENSE transaction"));

        Transaction income = transfer.getTransactions().stream()
                .filter(transaction -> transaction.getType() == TransactionType.INCOME)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Transfer " + transfer.getId() + " has no INCOME transaction"));

        if (expense.getAmount() == null || income.getAmount() == null
                || expense.getAmount().compareTo(income.getAmount()) != 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Transfer " + transfer.getId() + " has inconsistent transaction amounts");
        }

        LocalDateTime occurredAt = expense.getOccurredAt() != null ? expense.getOccurredAt() : income.getOccurredAt();
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }

        Account fromAccount = expense.getAccount();
        Account toAccount = income.getAccount();
        if (fromAccount == null || toAccount == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Transfer " + transfer.getId() + " has transactions without account");
        }

        return new TransferPair(expense, income, fromAccount, toAccount, expense.getAmount(), occurredAt);
    }

    private Transaction buildTransferTransaction(
            LocalDateTime occurredAt,
            BigDecimal amount,
            String description,
            TransactionType type,
            Transfer transfer,
            Account account) {
        Transaction transaction = new Transaction();
        transaction.setOccurredAt(occurredAt);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setType(type);
        transaction.setTransfer(transfer);
        transaction.setAccount(account);
        transaction.setTags(List.of());
        return transaction;
    }

    private record TransferPair(
            Transaction expense,
            Transaction income,
            Account fromAccount,
            Account toAccount,
            BigDecimal amount,
            LocalDateTime occurredAt) {
    }
}
