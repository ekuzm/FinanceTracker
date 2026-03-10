package com.finance.tracker.service.impl;

import com.finance.tracker.cache.CacheKey;
import com.finance.tracker.cache.CacheManager;
import com.finance.tracker.domain.Account;
import com.finance.tracker.domain.AccountType;
import com.finance.tracker.domain.Budget;
import com.finance.tracker.domain.User;
import com.finance.tracker.dto.request.UserRequest;
import com.finance.tracker.dto.request.UserUpdateRequest;
import com.finance.tracker.dto.response.UserResponse;
import com.finance.tracker.exception.ConflictException;
import com.finance.tracker.exception.ResourceNotFoundException;
import com.finance.tracker.mapper.UserMapper;
import com.finance.tracker.repository.AccountRepository;
import com.finance.tracker.repository.BudgetRepository;
import com.finance.tracker.repository.UserRepository;
import com.finance.tracker.service.UserService;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final BudgetRepository budgetRepository;
    private final UserMapper userMapper;
    private final CacheManager cacheManager;

    @Override
    public UserResponse getUserById(Long id) {
        CacheKey cacheKey = buildCacheKey("getUserById", id);
        return cacheManager.computeIfAbsent(cacheKey, () -> {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found " + id));
            return userMapper.toResponse(user);
        });
    }

    @Override
    public List<UserResponse> getAllUsers() {
        CacheKey cacheKey = buildCacheKey("getAllUsers");
        return cacheManager.computeIfAbsent(cacheKey, () -> toResponses(userRepository.findAll()));
    }

    @Override
    public List<UserResponse> searchUsersByAccountTypeWithJpql(
            AccountType accountType, BigDecimal minBudgetLimit, BigDecimal maxBudgetLimit) {
        CacheKey cacheKey = buildSearchCacheKey(
                "searchUsersByAccountTypeWithJpql", accountType, minBudgetLimit, maxBudgetLimit);
        return cacheManager.computeIfAbsent(cacheKey, () -> userRepository
                .findUsersByAccountTypeWithJpql(accountType, minBudgetLimit, maxBudgetLimit)
                .stream()
                .map(userMapper::toResponse)
                .toList());
    }

    @Override
    public List<UserResponse> searchUsersByAccountTypeWithNative(
            AccountType accountType, BigDecimal minBudgetLimit, BigDecimal maxBudgetLimit) {
        CacheKey cacheKey = buildSearchCacheKey(
                "searchUsersByAccountTypeWithNative", accountType, minBudgetLimit, maxBudgetLimit);
        return cacheManager.computeIfAbsent(cacheKey, () -> userRepository
                .findUsersByAccountTypeWithNative(accountType.name(), minBudgetLimit, maxBudgetLimit)
                .stream()
                .map(userMapper::toResponse)
                .toList());
    }

    @Override
    @Transactional
    public UserResponse createUser(UserRequest request) {
        List<Account> accounts = getAccounts(request.getAccountIds());
        List<Budget> budgets = getBudgets(request.getBudgetIds());
        ensureAssignableAccounts(accounts, null);
        ensureAssignableBudgets(budgets, null);
        User user = userMapper.fromRequest(request, accounts, budgets);
        User saved = userRepository.save(user);
        invalidateSearchCache();
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found " + id));
        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getAccountIds() != null) {
            List<Account> accounts = getAccounts(request.getAccountIds());
            ensureAssignableAccounts(accounts, user.getId());
            user.getAccounts().forEach(account -> account.setUser(null));
            user.setAccounts(accounts);
            accounts.forEach(account -> account.setUser(user));
        }
        if (request.getBudgetIds() != null) {
            List<Budget> budgets = getBudgets(request.getBudgetIds());
            ensureAssignableBudgets(budgets, user.getId());
            user.getBudgets().forEach(budget -> budget.setUser(null));
            user.setBudgets(budgets);
            budgets.forEach(budget -> budget.setUser(user));
        }
        User saved = userRepository.save(user);
        invalidateSearchCache();
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found " + id);
        }
        userRepository.deleteById(id);
        invalidateSearchCache();
    }

    private List<Account> getAccounts(List<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return List.of();
        }
        List<Account> accounts = accountRepository.findAllById(accountIds);
        if (accounts.size() != accountIds.size()) {
            throw new ResourceNotFoundException("Some accounts not found");
        }
        return accounts;
    }

    private List<Budget> getBudgets(List<Long> budgetIds) {
        if (budgetIds == null || budgetIds.isEmpty()) {
            return List.of();
        }
        List<Budget> budgets = budgetRepository.findAllById(budgetIds);
        if (budgets.size() != budgetIds.size()) {
            throw new ResourceNotFoundException("Some budgets not found");
        }
        return budgets;
    }

    private List<UserResponse> toResponses(List<User> users) {
        return users.stream().map(userMapper::toResponse).toList();
    }

    private CacheKey buildCacheKey(String methodName, Object... args) {
        return new CacheKey(User.class, methodName, args);
    }

    private CacheKey buildSearchCacheKey(
            String methodName, AccountType accountType, BigDecimal minBudgetLimit, BigDecimal maxBudgetLimit) {
        return buildCacheKey(methodName, accountType, minBudgetLimit, maxBudgetLimit);
    }

    private void invalidateSearchCache() {
        cacheManager.invalidate(User.class, Account.class, Budget.class);
    }

    private void ensureAssignableAccounts(List<Account> accounts, Long currentUserId) {
        for (Account account : accounts) {
            boolean hasOwner = account.getUser() != null;
            boolean belongsToCurrentUser =
                    currentUserId != null && hasOwner && currentUserId.equals(account.getUser().getId());

            if (hasOwner && !belongsToCurrentUser) {
                throw new ConflictException("Account " + account.getId() + " already belongs to another user");
            }
        }
    }

    private void ensureAssignableBudgets(List<Budget> budgets, Long currentUserId) {
        for (Budget budget : budgets) {
            boolean hasOwner = budget.getUser() != null;
            boolean belongsToCurrentUser =
                    currentUserId != null && hasOwner && currentUserId.equals(budget.getUser().getId());

            if (hasOwner && !belongsToCurrentUser) {
                throw new ConflictException("Budget " + budget.getId() + " already belongs to another user");
            }
        }
    }
}
