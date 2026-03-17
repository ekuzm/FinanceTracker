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
import com.finance.tracker.exception.ResourceNotFoundException;
import com.finance.tracker.mapper.BudgetMapper;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

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
    void getBudgetByIdShouldReturnMappedBudget() {
        Budget budget = budget(1L, user(3L));
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(budget));

        var response = service.getBudgetById(1L);

        assertEquals(1L, response.getId());
        assertEquals(3L, response.getUserId());
    }

    @Test
    void getBudgetByIdShouldThrowWhenMissing() {
        when(budgetRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getBudgetById(1L));
    }

    @Test
    void getAllBudgetsShouldReturnMappedPage() {
        when(budgetRepository.findAll(PageRequest.of(0, 2))).thenReturn(new PageImpl<>(List.of(budget(1L, user(2L)))));

        var page = service.getAllBudgets(PageRequest.of(0, 2));

        assertEquals(1, page.getContent().size());
        assertEquals(1L, page.getContent().get(0).getId());
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
    void createBudgetShouldThrowWhenUserIsMissing() {
        BudgetRequest request = new BudgetRequest(
                "Food",
                new BigDecimal("200.00"),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                1L);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.createBudget(request));
    }

    @Test
    void createBudgetShouldRejectNullStartDate() {
        BudgetRequest request =
                new BudgetRequest("Food", new BigDecimal("200.00"), null, LocalDate.of(2026, 3, 31), 1L);

        assertThrows(BadRequestException.class, () -> service.createBudget(request));
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

    @Test
    void updateBudgetShouldChangeOwnerAndInvalidateCache() {
        Budget budget = budget(5L, user(1L));
        User newUser = user(9L);
        BudgetUpdateRequest request = new BudgetUpdateRequest();
        request.setName("Travel");
        request.setLimitAmount(new BigDecimal("500.00"));
        request.setStartDate(LocalDate.of(2026, 4, 1));
        request.setEndDate(LocalDate.of(2026, 4, 30));
        request.setUserId(9L);

        when(budgetRepository.findById(5L)).thenReturn(Optional.of(budget));
        when(userRepository.findById(9L)).thenReturn(Optional.of(newUser));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateBudget(5L, request);

        assertEquals("Travel", response.getName());
        assertEquals(9L, response.getUserId());
        verify(cacheManager).invalidate(User.class, Account.class, Budget.class);
    }

    @Test
    void updateBudgetShouldThrowWhenBudgetIsMissing() {
        BudgetUpdateRequest request = new BudgetUpdateRequest();
        when(budgetRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateBudget(10L, request));
    }

    @Test
    void updateBudgetShouldRejectNullEndDateAfterPatch() {
        Budget budget = budget(5L, user(1L));
        budget.setEndDate(null);
        BudgetUpdateRequest request = new BudgetUpdateRequest();
        request.setStartDate(LocalDate.of(2026, 4, 1));

        when(budgetRepository.findById(5L)).thenReturn(Optional.of(budget));

        assertThrows(BadRequestException.class, () -> service.updateBudget(5L, request));
    }

    @Test
    void deleteBudgetShouldDeleteExistingBudget() {
        when(budgetRepository.findById(2L)).thenReturn(Optional.of(budget(2L, user(1L))));

        service.deleteBudget(2L);

        verify(budgetRepository).delete(any(Budget.class));
        verify(cacheManager).invalidate(User.class, Account.class, Budget.class);
    }

    @Test
    void deleteBudgetShouldThrowWhenMissing() {
        when(budgetRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteBudget(2L));
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
