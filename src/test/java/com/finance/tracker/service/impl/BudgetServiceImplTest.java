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
import com.finance.tracker.domain.Budget;
import com.finance.tracker.domain.User;
import com.finance.tracker.dto.request.BudgetRequest;
import com.finance.tracker.dto.request.BudgetUpdateRequest;
import com.finance.tracker.exception.BadRequestException;
import com.finance.tracker.mapper.BudgetMapper;
import com.finance.tracker.repository.BudgetRepository;
import com.finance.tracker.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BudgetServiceImplTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private UserRepository userRepository;

    private CacheManager cacheManager;
    private BudgetServiceImpl service;

    @BeforeEach
    void setUp() {
        cacheManager = spy(new CacheManager());
        service = new BudgetServiceImpl(budgetRepository, userRepository, new BudgetMapper(), cacheManager);
    }

    @Test
    void createBudgetShouldSaveBudgetAndInvalidateCache() {
        User user = user(1L);
        BudgetRequest request = new BudgetRequest(
                "Food",
                new BigDecimal("300.00"),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
            Budget budget = invocation.getArgument(0);
            budget.setId(10L);
            return budget;
        });

        var response = service.createBudget(request);

        assertEquals(10L, response.getId());
        assertEquals(1L, response.getUserId());
        verify(budgetRepository).save(any(Budget.class));
        verify(cacheManager).invalidate(User.class, Account.class, Budget.class);
    }

    @Test
    void createBudgetShouldRejectInvalidDateRange() {
        BudgetRequest request = new BudgetRequest(
                "Food",
                new BigDecimal("300.00"),
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 3, 1),
                1L);

        assertThrows(BadRequestException.class, () -> service.createBudget(request));

        verify(userRepository, never()).findById(any());
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void updateBudgetShouldMergeFieldsWithExistingState() {
        User user = user(1L);
        Budget budget = budget(5L, user);
        BudgetUpdateRequest request = new BudgetUpdateRequest();
        request.setLimitAmount(new BigDecimal("450.00"));
        request.setEndDate(LocalDate.of(2026, 4, 5));

        when(budgetRepository.findById(5L)).thenReturn(Optional.of(budget));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateBudget(5L, request);

        assertEquals(LocalDate.of(2026, 3, 1), response.getStartDate());
        assertEquals(LocalDate.of(2026, 4, 5), response.getEndDate());
        assertEquals(0, new BigDecimal("450.00").compareTo(response.getLimitAmount()));
        verify(cacheManager).invalidate(User.class, Account.class, Budget.class);
    }

    private static User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private static Budget budget(Long id, User user) {
        Budget budget = new Budget();
        budget.setId(id);
        budget.setName("Budget");
        budget.setLimitAmount(new BigDecimal("300.00"));
        budget.setStartDate(LocalDate.of(2026, 3, 1));
        budget.setEndDate(LocalDate.of(2026, 3, 31));
        budget.setUser(user);
        return budget;
    }
}
