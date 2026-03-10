package com.finance.tracker.service.impl;

import com.finance.tracker.cache.CacheManager;
import com.finance.tracker.domain.Account;
import com.finance.tracker.domain.Budget;
import com.finance.tracker.domain.User;
import com.finance.tracker.dto.request.BudgetRequest;
import com.finance.tracker.dto.response.BudgetResponse;
import com.finance.tracker.mapper.BudgetMapper;
import com.finance.tracker.repository.BudgetRepository;
import com.finance.tracker.repository.UserRepository;
import com.finance.tracker.service.BudgetService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class BudgetServiceImpl implements BudgetService {

    private static final String BUDGET_NOT_FOUND_MESSAGE = "Budget not found ";

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final BudgetMapper budgetMapper;
    private final CacheManager cacheManager;

    @Override
    public BudgetResponse getBudgetById(Long id) {
        return executeWithLogging("getBudgetById", () -> {
            Budget budget = budgetRepository.findById(id).orElseThrow(
                    () -> new ResponseStatusException(HttpStatus.NOT_FOUND, BUDGET_NOT_FOUND_MESSAGE + id));
            return budgetMapper.toResponse(budget);
        });
    }

    @Override
    public Page<BudgetResponse> getAllBudgets(Pageable pageable) {
        return executeWithLogging(
                "getAllBudgets", () -> budgetRepository.findAll(pageable).map(budgetMapper::toResponse));
    }

    @Override
    @Transactional
    public BudgetResponse createBudget(BudgetRequest request) {
        return executeWithLogging("createBudget", () -> {
            validateDateRange(request.getStartDate(), request.getEndDate());
            User user = getUser(request.getUserId());
            Budget budget = budgetMapper.fromRequest(request, user);
            Budget saved = budgetRepository.save(budget);
            invalidateSearchCache();
            return budgetMapper.toResponse(saved);
        });
    }

    @Override
    @Transactional
    public BudgetResponse updateBudget(Long id, BudgetRequest request) {
        return executeWithLogging("updateBudget", () -> {
            Budget budget = budgetRepository.findById(id).orElseThrow(
                    () -> new ResponseStatusException(HttpStatus.NOT_FOUND, BUDGET_NOT_FOUND_MESSAGE + id));
            if (request.getName() != null) {
                budget.setName(request.getName());
            }
            if (request.getLimitAmount() != null) {
                budget.setLimitAmount(request.getLimitAmount());
            }
            if (request.getStartDate() != null) {
                budget.setStartDate(request.getStartDate());
            }
            if (request.getEndDate() != null) {
                budget.setEndDate(request.getEndDate());
            }
            validateDateRange(budget.getStartDate(), budget.getEndDate());

            if (request.getUserId() != null) {
                budget.setUser(getUser(request.getUserId()));
            }

            Budget saved = budgetRepository.save(budget);
            invalidateSearchCache();
            return budgetMapper.toResponse(saved);
        });
    }

    @Override
    @Transactional
    public void deleteBudget(Long id) {
        executeWithLogging("deleteBudget", () -> {
            Budget budget = budgetRepository.findById(id).orElseThrow(
                    () -> new ResponseStatusException(HttpStatus.NOT_FOUND, BUDGET_NOT_FOUND_MESSAGE + id));
            budgetRepository.delete(budget);
            invalidateSearchCache();
        });
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId));
    }

    private void validateDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Budget startDate and endDate are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Budget startDate must be <= endDate");
        }
    }

    private <T> T executeWithLogging(String methodName, java.util.function.Supplier<T> action) {
        long startTime = System.currentTimeMillis();
        try {
            T result = action.get();
            long executionTimeMs = System.currentTimeMillis() - startTime;
            log.debug("Method BudgetServiceImpl.{} completed in {} ms", methodName, executionTimeMs);
            return result;
        } catch (RuntimeException exception) {
            long executionTimeMs = System.currentTimeMillis() - startTime;
            log.debug(
                    "Method BudgetServiceImpl.{} failed in {} ms: {}",
                    methodName,
                    executionTimeMs,
                    exception.getMessage());
            throw exception;
        }
    }

    private void executeWithLogging(String methodName, Runnable action) {
        long startTime = System.currentTimeMillis();
        try {
            action.run();
            long executionTimeMs = System.currentTimeMillis() - startTime;
            log.debug("Method BudgetServiceImpl.{} completed in {} ms", methodName, executionTimeMs);
        } catch (RuntimeException exception) {
            long executionTimeMs = System.currentTimeMillis() - startTime;
            log.debug(
                    "Method BudgetServiceImpl.{} failed in {} ms: {}",
                    methodName,
                    executionTimeMs,
                    exception.getMessage());
            throw exception;
        }
    }

    private void invalidateSearchCache() {
        cacheManager.invalidate(User.class, Account.class, Budget.class);
    }
}
