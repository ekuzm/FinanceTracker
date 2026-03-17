package com.finance.tracker.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finance.tracker.cache.CacheManager;
import com.finance.tracker.domain.Account;
import com.finance.tracker.domain.AccountType;
import com.finance.tracker.domain.Budget;
import com.finance.tracker.domain.User;
import com.finance.tracker.dto.request.UserRequest;
import com.finance.tracker.dto.request.UserUpdateRequest;
import com.finance.tracker.exception.ConflictException;
import com.finance.tracker.exception.ResourceNotFoundException;
import com.finance.tracker.mapper.UserMapper;
import com.finance.tracker.repository.AccountRepository;
import com.finance.tracker.repository.BudgetRepository;
import com.finance.tracker.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private BudgetRepository budgetRepository;

    private CacheManager cacheManager;
    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        cacheManager = spy(new CacheManager());
        service = new UserServiceImpl(
                userRepository,
                accountRepository,
                budgetRepository,
                new UserMapper(),
                cacheManager);
    }

    @Test
    void createUserShouldAssignAccountsAndBudgetsAndInvalidateCache() {
        Account account = account(1L, null);
        Budget budget = budget(5L, null);
        UserRequest request = new UserRequest("alex", "alex@example.com", List.of(1L), List.of(5L));

        when(accountRepository.findAllById(List.of(1L))).thenReturn(List.of(account));
        when(budgetRepository.findAllById(List.of(5L))).thenReturn(List.of(budget));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });

        var response = service.createUser(request);

        assertEquals(10L, response.getId());
        assertIterableEquals(List.of(1L), response.getAccountIds());
        assertIterableEquals(List.of(5L), response.getBudgetIds());
        assertEquals(10L, account.getUser().getId());
        assertEquals(10L, budget.getUser().getId());
        verify(cacheManager).invalidate(User.class, Account.class, Budget.class);
    }

    @Test
    void createUserShouldRejectAccountOwnedByAnotherUser() {
        Account foreignAccount = account(1L, user(99L));
        UserRequest request = new UserRequest("alex", "alex@example.com", List.of(1L), List.of());

        when(accountRepository.findAllById(List.of(1L))).thenReturn(List.of(foreignAccount));

        assertThrows(ConflictException.class, () -> service.createUser(request));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUserShouldHandleNullAndEmptyRelationIdsWithoutRepositoryLookups() {
        AtomicLong ids = new AtomicLong(10L);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(ids.getAndIncrement());
            return user;
        });

        var firstResponse = service.createUser(new UserRequest("alex", "alex@example.com", null, List.of()));
        var secondResponse = service.createUser(new UserRequest("sam", "sam@example.com", List.of(), null));

        assertIterableEquals(List.of(), firstResponse.getAccountIds());
        assertIterableEquals(List.of(), firstResponse.getBudgetIds());
        assertIterableEquals(List.of(), secondResponse.getAccountIds());
        assertIterableEquals(List.of(), secondResponse.getBudgetIds());
        verify(accountRepository, never()).findAllById(any());
        verify(budgetRepository, never()).findAllById(any());
    }

    @Test
    void createUserShouldThrowWhenSomeAccountsAreMissing() {
        when(accountRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(account(1L, null)));

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.createUser(new UserRequest("alex", "alex@example.com", List.of(1L, 2L), List.of())));
    }

    @Test
    void createUserShouldThrowWhenSomeBudgetsAreMissing() {
        when(budgetRepository.findAllById(List.of(5L, 6L))).thenReturn(List.of(budget(5L, null)));

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.createUser(new UserRequest("alex", "alex@example.com", List.of(), List.of(5L, 6L))));
    }

    @Test
    void createUserShouldRejectBudgetOwnedByAnotherUser() {
        Budget foreignBudget = budget(7L, user(99L, "owner", "owner@example.com"));

        when(budgetRepository.findAllById(List.of(7L))).thenReturn(List.of(foreignBudget));

        assertThrows(
                ConflictException.class,
                () -> service.createUser(new UserRequest("alex", "alex@example.com", List.of(), List.of(7L))));
    }

    @Test
    void getUserByIdShouldUseCacheAndCallRepositoryOnce() {
        User user = user(1L, "alex", "alex@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        var firstResponse = service.getUserById(1L);
        var secondResponse = service.getUserById(1L);

        assertEquals("alex", firstResponse.getUsername());
        assertEquals("alex@example.com", secondResponse.getEmail());
        verify(userRepository).findById(1L);
    }

    @Test
    void getUserByIdShouldThrowWhenUserIsMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getUserById(99L));
    }

    @Test
    void getAllUsersShouldReturnMappedUsersAndUseCache() {
        User user = user(1L, "alex", "alex@example.com");
        user.setAccounts(List.of(account(11L, user)));
        user.setBudgets(List.of(budget(21L, user)));

        when(userRepository.findAll()).thenReturn(List.of(user));

        var firstResponse = service.getAllUsers();
        var secondResponse = service.getAllUsers();

        assertEquals(1, firstResponse.size());
        assertEquals("alex", firstResponse.get(0).getUsername());
        assertIterableEquals(List.of(11L), firstResponse.get(0).getAccountIds());
        assertIterableEquals(List.of(21L), secondResponse.get(0).getBudgetIds());
        verify(userRepository).findAll();
    }

    @Test
    void searchUsersShouldCacheBothJpqlAndNativeVariants() {
        User user = user(2L, "sam", "sam@example.com");

        when(userRepository.findUsersByAccountTypeWithJpql(
                        AccountType.CHECKING, new BigDecimal("100.00"), new BigDecimal("500.00")))
                .thenReturn(List.of(user));
        when(userRepository.findUsersByAccountTypeWithNative(
                        "SAVINGS", new BigDecimal("50.00"), new BigDecimal("300.00")))
                .thenReturn(List.of(user));

        var jpqlFirst = service.searchUsersByAccountTypeWithJpql(
                AccountType.CHECKING, new BigDecimal("100.00"), new BigDecimal("500.00"));
        var jpqlSecond = service.searchUsersByAccountTypeWithJpql(
                AccountType.CHECKING, new BigDecimal("100.00"), new BigDecimal("500.00"));
        var nativeFirst = service.searchUsersByAccountTypeWithNative(
                AccountType.SAVINGS, new BigDecimal("50.00"), new BigDecimal("300.00"));
        var nativeSecond = service.searchUsersByAccountTypeWithNative(
                AccountType.SAVINGS, new BigDecimal("50.00"), new BigDecimal("300.00"));

        assertEquals(1, jpqlFirst.size());
        assertEquals(1, jpqlSecond.size());
        assertEquals(1, nativeFirst.size());
        assertEquals(1, nativeSecond.size());
        verify(userRepository).findUsersByAccountTypeWithJpql(
                AccountType.CHECKING, new BigDecimal("100.00"), new BigDecimal("500.00"));
        verify(userRepository).findUsersByAccountTypeWithNative(
                "SAVINGS", new BigDecimal("50.00"), new BigDecimal("300.00"));
    }

    @Test
    void updateUserShouldPatchScalarFieldsWhenRelationsAreOmitted() {
        User user = user(5L, "alex", "alex@example.com");
        UserUpdateRequest request = new UserUpdateRequest();
        request.setUsername("sam");
        request.setEmail("sam@example.com");

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateUser(5L, request);

        assertEquals("sam", response.getUsername());
        assertEquals("sam@example.com", response.getEmail());
        verify(accountRepository, never()).findAllById(any());
        verify(budgetRepository, never()).findAllById(any());
    }

    @Test
    void updateUserShouldReassignAccountsAndBudgetsOwnedByCurrentUser() {
        User user = user(5L, "alex", "alex@example.com");
        Account previousAccount = account(1L, user);
        Budget previousBudget = budget(2L, user);
        user.setAccounts(List.of(previousAccount));
        user.setBudgets(List.of(previousBudget));

        Account newAccount = account(10L, user);
        Budget newBudget = budget(20L, user);
        UserUpdateRequest request = new UserUpdateRequest();
        request.setAccountIds(List.of(10L));
        request.setBudgetIds(List.of(20L));

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(accountRepository.findAllById(List.of(10L))).thenReturn(List.of(newAccount));
        when(budgetRepository.findAllById(List.of(20L))).thenReturn(List.of(newBudget));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateUser(5L, request);

        assertEquals(user, newAccount.getUser());
        assertEquals(user, newBudget.getUser());
        assertNull(previousAccount.getUser());
        assertNull(previousBudget.getUser());
        assertIterableEquals(List.of(10L), response.getAccountIds());
        assertIterableEquals(List.of(20L), response.getBudgetIds());
    }

    @Test
    void updateUserShouldThrowWhenUserIsMissing() {
        when(userRepository.findById(77L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateUser(77L, new UserUpdateRequest()));
    }

    @Test
    void updateUserShouldRejectAccountBelongingToAnotherUser() {
        User user = user(5L, "alex", "alex@example.com");
        Account foreignAccount = account(10L, user(8L, "owner", "owner@example.com"));
        UserUpdateRequest request = new UserUpdateRequest();
        request.setAccountIds(List.of(10L));

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(accountRepository.findAllById(List.of(10L))).thenReturn(List.of(foreignAccount));

        assertThrows(ConflictException.class, () -> service.updateUser(5L, request));
    }

    @Test
    void updateUserShouldRejectBudgetBelongingToAnotherUser() {
        User user = user(5L, "alex", "alex@example.com");
        Budget foreignBudget = budget(20L, user(8L, "owner", "owner@example.com"));
        UserUpdateRequest request = new UserUpdateRequest();
        request.setBudgetIds(List.of(20L));

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(budgetRepository.findAllById(List.of(20L))).thenReturn(List.of(foreignBudget));

        assertThrows(ConflictException.class, () -> service.updateUser(5L, request));
    }

    @Test
    void updateUserShouldAllowUnownedAccountsAndBudgetsForExistingUser() {
        User user = user(5L, "alex", "alex@example.com");
        Account account = account(10L, null);
        Budget budget = budget(20L, null);
        UserUpdateRequest request = new UserUpdateRequest();
        request.setAccountIds(List.of(10L));
        request.setBudgetIds(List.of(20L));

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(accountRepository.findAllById(List.of(10L))).thenReturn(List.of(account));
        when(budgetRepository.findAllById(List.of(20L))).thenReturn(List.of(budget));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateUser(5L, request);

        assertEquals(user, account.getUser());
        assertEquals(user, budget.getUser());
        assertIterableEquals(List.of(10L), response.getAccountIds());
        assertIterableEquals(List.of(20L), response.getBudgetIds());
    }

    @Test
    void deleteUserShouldDeleteExistingUserAndInvalidateCache() {
        when(userRepository.existsById(3L)).thenReturn(true);

        service.deleteUser(3L);

        verify(userRepository).deleteById(3L);
        verify(cacheManager).invalidate(User.class, Account.class, Budget.class);
    }

    @Test
    void deleteUserShouldThrowWhenUserIsMissing() {
        when(userRepository.existsById(3L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.deleteUser(3L));
    }

    private static User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private static User user(Long id, String username, String email) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        return user;
    }

    private static Account account(Long id, User owner) {
        Account account = new Account();
        account.setId(id);
        account.setName("Account " + id);
        account.setType(AccountType.CHECKING);
        account.setBalance(new BigDecimal("100.00"));
        account.setUser(owner);
        return account;
    }

    private static Budget budget(Long id, User owner) {
        Budget budget = new Budget();
        budget.setId(id);
        budget.setName("Budget " + id);
        budget.setLimitAmount(new BigDecimal("300.00"));
        budget.setStartDate(LocalDate.of(2026, 3, 1));
        budget.setEndDate(LocalDate.of(2026, 3, 31));
        budget.setUser(owner);
        return budget;
    }
}
