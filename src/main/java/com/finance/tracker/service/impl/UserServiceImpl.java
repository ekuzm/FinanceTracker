package com.finance.tracker.service.impl;

import com.finance.tracker.domain.Account;
import com.finance.tracker.domain.Transaction;
import com.finance.tracker.domain.User;
import com.finance.tracker.dto.request.UserRequest;
import com.finance.tracker.dto.response.UserResponse;
import com.finance.tracker.mapper.UserMapper;
import com.finance.tracker.repository.AccountRepository;
import com.finance.tracker.repository.TransactionRepository;
import com.finance.tracker.repository.UserRepository;
import com.finance.tracker.service.UserService;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserMapper userMapper;

    @Override
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found " + id));
        return userMapper.toResponse(user);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return toResponses(userRepository.findAll());
    }

    @Override
    @Transactional
    public UserResponse createUser(UserRequest request) {
        List<Account> accounts = getAccountsIfPresent(request.getAccountIds());
        List<Transaction> transactions = getTransactionsIfPresent(request.getTransactionIds());
        ensureAssignableAccounts(accounts, null);
        ensureAssignableTransactions(transactions, null);
        User user = userMapper.fromRequest(request, accounts, transactions);
        User saved = userRepository.save(user);
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found " + id));
        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getAccountIds() != null) {
            List<Account> accounts = getAccounts(request.getAccountIds());
            ensureAssignableAccounts(accounts, user.getId());
            user.getAccounts().forEach(a -> a.setUser(null));
            user.setAccounts(accounts);
            accounts.forEach(a -> a.setUser(user));
        }
        if (request.getTransactionIds() != null) {
            List<Transaction> transactions = getTransactions(request.getTransactionIds());
            ensureAssignableTransactions(transactions, user.getId());
            user.getTransactions().forEach(t -> t.setUser(null));
            user.setTransactions(transactions);
            transactions.forEach(t -> t.setUser(user));
        }
        User saved = userRepository.save(user);
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found " + id);
        }
        userRepository.deleteById(id);
    }

    private List<Account> getAccounts(List<Long> accountIds) {
        List<Account> accounts = accountRepository.findAllById(accountIds);
        if (accounts.size() != accountIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Some accounts not found");
        }
        return accounts;
    }

    private List<Account> getAccountsIfPresent(List<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return List.of();
        }
        return getAccounts(accountIds);
    }

    private List<Transaction> getTransactions(List<Long> transactionIds) {
        List<Transaction> transactions = transactionRepository.findAllById(transactionIds);
        if (transactions.size() != transactionIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Some transactions not found");
        }
        return transactions;
    }

    private List<Transaction> getTransactionsIfPresent(List<Long> transactionIds) {
        if (transactionIds == null || transactionIds.isEmpty()) {
            return List.of();
        }
        return getTransactions(transactionIds);
    }

    private List<UserResponse> toResponses(List<User> users) {
        return users.stream().map(userMapper::toResponse).toList();
    }

    private void ensureAssignableAccounts(List<Account> accounts, Long currentUserId) {
        for (Account account : accounts) {
            boolean hasOwner = account.getUser() != null;
            boolean belongsToCurrentUser =
                    currentUserId != null && hasOwner && currentUserId.equals(account.getUser().getId());

            if (hasOwner && !belongsToCurrentUser) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Account " + account.getId() + " already belongs to another user");
            }
        }
    }

    private void ensureAssignableTransactions(List<Transaction> transactions, Long currentUserId) {
        for (Transaction transaction : transactions) {
            boolean hasOwner = transaction.getUser() != null;
            boolean belongsToCurrentUser = currentUserId != null && hasOwner
                    && currentUserId.equals(transaction.getUser().getId());

            if (hasOwner && !belongsToCurrentUser) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Transaction " + transaction.getId() + " already belongs to another user");
            }
        }
    }
}
