package com.finance.tracker.service.impl;

import com.finance.tracker.cache.CacheManager;
import com.finance.tracker.domain.Account;
import com.finance.tracker.domain.Budget;
import com.finance.tracker.domain.Transaction;
import com.finance.tracker.domain.TransactionType;
import com.finance.tracker.domain.User;
import com.finance.tracker.dto.request.AccountTransferRequest;
import com.finance.tracker.dto.request.AccountRequest;
import com.finance.tracker.dto.request.AccountUpdateRequest;
import com.finance.tracker.dto.response.AccountResponse;
import com.finance.tracker.exception.BadRequestException;
import com.finance.tracker.exception.ConflictException;
import com.finance.tracker.exception.ResourceNotFoundException;
import com.finance.tracker.mapper.AccountMapper;
import com.finance.tracker.repository.AccountRepository;
import com.finance.tracker.repository.TransactionRepository;
import com.finance.tracker.repository.UserRepository;
import com.finance.tracker.service.AccountService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private static final String ACCOUNT_NOT_FOUND_MESSAGE = "Account not found ";
    private static final String DEFAULT_TRANSFER_NOTE = "Transfer";

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final AccountMapper accountMapper;
    private final CacheManager cacheManager;

    @Override
    public AccountResponse getAccountById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ACCOUNT_NOT_FOUND_MESSAGE + id));
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
        invalidateSearchCache();
        return accountMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void createTransferTx(AccountTransferRequest request, boolean failAfterDebit) {
        executeTransfer(request, failAfterDebit);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void createTransferNoTx(AccountTransferRequest request, boolean failAfterDebit) {
        executeTransfer(request, failAfterDebit);
    }

    @Override
    @Transactional
    public AccountResponse updateAccount(Long id, AccountUpdateRequest request) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ACCOUNT_NOT_FOUND_MESSAGE + id));
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
                throw new ConflictException(
                        "Cannot change account owner while account has transactions");
            }
            account.setUser(newOwner);
        }
        Account saved = accountRepository.save(account);
        invalidateSearchCache();
        return accountMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ACCOUNT_NOT_FOUND_MESSAGE + id));
        accountRepository.delete(account);
        invalidateSearchCache();
    }

    private void executeTransfer(AccountTransferRequest request, boolean failAfterDebit) {
        validatePositiveAmount(request.getAmount());

        Account fromAccount = getAccount(request.getFromAccountId());
        Account toAccount = getAccount(request.getToAccountId());

        validateAccountsDistinct(fromAccount, toAccount);
        validateTransferOwnership(fromAccount, toAccount);
        validateSufficientFunds(fromAccount, request.getAmount());

        LocalDateTime occurredAt = request.getOccurredAt() != null ? request.getOccurredAt() : LocalDateTime.now();
        String note = normalizeTransferNote(request.getNote());

        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        accountRepository.save(fromAccount);

        transactionRepository.save(buildTransferTransaction(
                occurredAt,
                request.getAmount(),
                note + " to account " + toAccount.getId(),
                TransactionType.EXPENSE,
                fromAccount));

        if (failAfterDebit) {
            throw new IllegalStateException(
                    "Forced error after debit operation");
        }

        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));
        accountRepository.save(toAccount);

        transactionRepository.save(buildTransferTransaction(
                occurredAt,
                request.getAmount(),
                note + " from account " + fromAccount.getId(),
                TransactionType.INCOME,
                toAccount));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found " + userId));
    }

    private Account getAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(ACCOUNT_NOT_FOUND_MESSAGE + accountId));
    }

    private void validateAccountsDistinct(Account fromAccount, Account toAccount) {
        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new BadRequestException("Source and target account must be different");
        }
    }

    private void validateTransferOwnership(Account fromAccount, Account toAccount) {
        Long fromUserId = fromAccount.getUser() != null ? fromAccount.getUser().getId() : null;
        Long toUserId = toAccount.getUser() != null ? toAccount.getUser().getId() : null;
        if (fromUserId == null || !fromUserId.equals(toUserId)) {
            throw new ConflictException("Both accounts must belong to the same user");
        }
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Transfer amount must be > 0");
        }
    }

    private void validateSufficientFunds(Account fromAccount, BigDecimal amount) {
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new ConflictException("Insufficient funds on source account " + fromAccount.getId());
        }
    }

    private String normalizeTransferNote(String note) {
        if (note == null || note.isBlank()) {
            return DEFAULT_TRANSFER_NOTE;
        }
        String normalized = note.trim();
        if (normalized.length() > 255) {
            throw new BadRequestException("Transfer note length must be <= 255");
        }
        return normalized;
    }

    private Transaction buildTransferTransaction(
            LocalDateTime occurredAt,
            BigDecimal amount,
            String description,
            TransactionType type,
            Account account) {
        Transaction transaction = new Transaction();
        transaction.setOccurredAt(occurredAt);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setType(type);
        transaction.setAccount(account);
        transaction.setTags(List.of());
        return transaction;
    }

    private List<AccountResponse> toResponses(List<Account> accounts) {
        return accounts.stream().map(accountMapper::toResponse).toList();
    }

    private void invalidateSearchCache() {
        cacheManager.invalidate(User.class, Account.class, Budget.class);
    }
}
