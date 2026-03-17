package com.finance.tracker.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.finance.tracker.exception.ConflictException;
import com.finance.tracker.mapper.UserMapper;
import com.finance.tracker.repository.AccountRepository;
import com.finance.tracker.repository.BudgetRepository;
import com.finance.tracker.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
        assertEquals(List.of(1L), response.getAccountIds());
        assertEquals(List.of(5L), response.getBudgetIds());
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
    void getUserByIdShouldUseCacheAndCallRepositoryOnce() {
        User user = user(1L);
        user.setUsername("alex");
        user.setEmail("alex@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        var firstResponse = service.getUserById(1L);
        var secondResponse = service.getUserById(1L);

        assertEquals("alex", firstResponse.getUsername());
        assertEquals("alex@example.com", secondResponse.getEmail());
        verify(userRepository).findById(1L);
    }

    private static User user(Long id) {
        User user = new User();
        user.setId(id);
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
        budget.setLimitAmount(new BigDecimal("400.00"));
        budget.setStartDate(LocalDate.of(2026, 3, 1));
        budget.setEndDate(LocalDate.of(2026, 3, 31));
        budget.setUser(owner);
        return budget;
    }
}
