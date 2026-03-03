package com.finance.tracker.service.impl;

import com.finance.tracker.domain.Account;
import com.finance.tracker.domain.Transaction;
import com.finance.tracker.domain.TransactionType;
import com.finance.tracker.domain.User;
import com.finance.tracker.dto.request.AccountRequest;
import com.finance.tracker.dto.request.TransferRequest;
import com.finance.tracker.dto.response.AccountResponse;
import com.finance.tracker.mapper.AccountMapper;
import com.finance.tracker.repository.AccountRepository;
import com.finance.tracker.repository.TransactionRepository;
import com.finance.tracker.repository.UserRepository;
import com.finance.tracker.service.AccountService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
public class AccountServiceImpl implements AccountService {

    private static final String ACCOUNT_NOT_FOUND_MESSAGE = "Account not found ";

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final AccountMapper accountMapper;

    @Override
    public AccountResponse getAccountById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND_MESSAGE + id));
        return accountMapper.toResponse(account);
    }

    @Override
    public List<AccountResponse> getAllAccounts() {
        return toResponses(accountRepository.findAll());
    }

    @Override
    @Transactional
    public AccountResponse createAccount(AccountRequest request) {
        Account account = accountMapper.fromRequest(request);
        account.setUser(getUser(request.getUserId()));
        Account saved = accountRepository.save(account);
        return accountMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AccountResponse updateAccount(Long id, AccountRequest request) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND_MESSAGE + id));
        if (request.getName() != null) {
            account.setName(request.getName());
        }
        if (request.getType() != null) {
            account.setType(request.getType());
        }
        if (request.getBalance() != null) {
            account.setBalance(request.getBalance());
        }
        if (request.getUserId() != null) {
            User newOwner = getUser(request.getUserId());
            Long currentOwnerId = account.getUser() != null ? account.getUser().getId() : null;
            if (currentOwnerId != null
                    && !currentOwnerId.equals(newOwner.getId())
                    && transactionRepository.existsByAccountId(account.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Cannot change account owner while account has transactions");
            }
            account.setUser(newOwner);
        }
        Account saved = accountRepository.save(account);
        return accountMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND_MESSAGE + id));
        if (transactionRepository.existsByAccountId(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete account with existing transactions: " + id);
        }
        accountRepository.delete(account);
    }

    @Override
    @Transactional
    public void transferTx(TransferRequest request, boolean failAfterDebit) {
        executeTransfer(request, failAfterDebit);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void transferNoTx(TransferRequest request, boolean failAfterDebit) {
        executeTransfer(request, failAfterDebit);
    }

    private void executeTransfer(TransferRequest request, boolean failAfterDebit) {
        if (request.getFromAccountId().equals(request.getToAccountId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source and target account must be different");
        }

        User user = getUser(request.getUserId());
        Account fromAccount = getAccount(request.getFromAccountId());
        Account toAccount = getAccount(request.getToAccountId());

        validateTransferOwnership(user, fromAccount, toAccount);
        validateSufficientFunds(fromAccount, request.getAmount());

        UUID transferId = UUID.randomUUID();
        LocalDateTime occurredAt = request.getOccurredAt() != null ? request.getOccurredAt() : LocalDateTime.now();
        String description = request.getDescription() != null && !request.getDescription().isBlank()
                ? request.getDescription().trim()
                : "Transfer";

        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        accountRepository.save(fromAccount);
        transactionRepository.save(buildTransferTransaction(
                occurredAt,
                request.getAmount(),
                description + " to account " + toAccount.getId(),
                TransactionType.EXPENSE,
                transferId,
                fromAccount,
                user));

        if (failAfterDebit) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Forced error after debit operation");
        }

        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));
        accountRepository.save(toAccount);
        transactionRepository.save(buildTransferTransaction(
                occurredAt,
                request.getAmount(),
                description + " from account " + fromAccount.getId(),
                TransactionType.INCOME,
                transferId,
                toAccount,
                user));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found " + userId));
    }

    private Account getAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND_MESSAGE + accountId));
    }

    private void validateTransferOwnership(User user, Account fromAccount, Account toAccount) {
        Long userId = user.getId();
        boolean fromOwnedByUser = fromAccount.getUser() != null && userId.equals(fromAccount.getUser().getId());
        boolean toOwnedByUser = toAccount.getUser() != null && userId.equals(toAccount.getUser().getId());
        if (!fromOwnedByUser || !toOwnedByUser) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Both accounts must belong to the same user as transfer request");
        }
    }

    private void validateSufficientFunds(Account fromAccount, BigDecimal amount) {
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Insufficient funds on source account " + fromAccount.getId());
        }
    }

    private Transaction buildTransferTransaction(
            LocalDateTime occurredAt,
            BigDecimal amount,
            String description,
            TransactionType type,
            UUID transferId,
            Account account,
            User user) {
        Transaction transaction = new Transaction();
        transaction.setOccurredAt(occurredAt);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setType(type);
        transaction.setTransferId(transferId);
        transaction.setAccount(account);
        transaction.setUser(user);
        transaction.setBudget(null);
        transaction.setTags(List.of());
        return transaction;
    }

    private List<AccountResponse> toResponses(List<Account> accounts) {
        return accounts.stream().map(accountMapper::toResponse).toList();
    }
}
