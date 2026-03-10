package com.finance.tracker.service.impl;

import com.finance.tracker.cache.CacheManager;
import com.finance.tracker.domain.Account;
import com.finance.tracker.domain.Budget;
import com.finance.tracker.domain.User;
import com.finance.tracker.dto.request.BudgetRequest;
import com.finance.tracker.dto.request.BudgetUpdateRequest;
import com.finance.tracker.dto.response.BudgetResponse;
import com.finance.tracker.exception.BadRequestException;
import com.finance.tracker.exception.ResourceNotFoundException;
import com.finance.tracker.mapper.BudgetMapper;
import com.finance.tracker.repository.BudgetRepository;
import com.finance.tracker.repository.UserRepository;
import com.finance.tracker.service.BudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private static final String BUDGET_NOT_FOUND_MESSAGE = "Budget not found ";

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final BudgetMapper budgetMapper;
    private final CacheManager cacheManager;

    @Override
    public BudgetResponse getBudgetById(Long id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(BUDGET_NOT_FOUND_MESSAGE + id));
        return budgetMapper.toResponse(budget);
    }

    @Override
    public Page<BudgetResponse> getAllBudgets(Pageable pageable) {
        return budgetRepository.findAll(pageable)
                .map(budgetMapper::toResponse);
    }

    @Override
    @Transactional
    public BudgetResponse createBudget(BudgetRequest request) {
        validateDateRange(request.getStartDate(), request.getEndDate());
        User user = getUser(request.getUserId());
        Budget budget = budgetMapper.fromRequest(request, user);
        Budget saved = budgetRepository.save(budget);
        invalidateSearchCache();
        return budgetMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BudgetResponse updateBudget(Long id, BudgetUpdateRequest request) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(BUDGET_NOT_FOUND_MESSAGE + id));
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
    }

    @Override
    @Transactional
    public void deleteBudget(Long id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(BUDGET_NOT_FOUND_MESSAGE + id));
        budgetRepository.delete(budget);
        invalidateSearchCache();
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private void validateDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BadRequestException("Budget startDate and endDate are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("Budget startDate must be <= endDate");
        }
    }

    private void invalidateSearchCache() {
        cacheManager.invalidate(User.class, Account.class, Budget.class);
    }
}
