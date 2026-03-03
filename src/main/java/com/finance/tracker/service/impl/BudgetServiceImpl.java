package com.finance.tracker.service.impl;

import com.finance.tracker.domain.Budget;
import com.finance.tracker.domain.TransactionType;
import com.finance.tracker.domain.User;
import com.finance.tracker.dto.request.BudgetRequest;
import com.finance.tracker.dto.response.BudgetResponse;
import com.finance.tracker.mapper.BudgetMapper;
import com.finance.tracker.repository.BudgetRepository;
import com.finance.tracker.repository.TransactionRepository;
import com.finance.tracker.repository.UserRepository;
import com.finance.tracker.service.BudgetService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetMapper budgetMapper;

    @Override
    public BudgetResponse getBudgetById(Long id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found " + id));
        return toResponse(budget, true);
    }

    @Override
    public List<BudgetResponse> getAllBudgets() {
        return budgetRepository.findAll().stream()
                .map(budget -> toResponse(budget, false))
                .toList();
    }

    @Override
    @Transactional
    public BudgetResponse createBudget(BudgetRequest request) {
        validateDateRange(request.getStartDate(), request.getEndDate());
        User user = getUser(request.getUserId());
        Budget budget = budgetMapper.fromRequest(request, user);
        Budget saved = budgetRepository.save(budget);
        return toResponse(saved, true);
    }

    @Override
    @Transactional
    public BudgetResponse updateBudget(Long id, BudgetRequest request) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found " + id));
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
            User newOwner = getUser(request.getUserId());
            Long currentOwnerId = budget.getUser() != null ? budget.getUser().getId() : null;
            if (currentOwnerId != null
                    && !currentOwnerId.equals(newOwner.getId())
                    && transactionRepository.existsByBudgetId(budget.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Cannot change budget owner while budget has transactions");
            }
            budget.setUser(newOwner);
        }

        if (transactionRepository.existsOutsideBudgetPeriod(
                budget.getId(), atDayStart(budget.getStartDate()), atDayEnd(budget.getEndDate()))) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot update budget period: some transactions are outside new date range");
        }

        Budget saved = budgetRepository.save(budget);
        return toResponse(saved, true);
    }

    @Override
    @Transactional
    public void deleteBudget(Long id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found " + id));
        if (transactionRepository.existsByBudgetId(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete budget with existing transactions: " + id);
        }
        budgetRepository.delete(budget);
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

    private LocalDateTime atDayStart(java.time.LocalDate date) {
        return date.atStartOfDay();
    }

    private LocalDateTime atDayEnd(java.time.LocalDate date) {
        return date.atTime(23, 59, 59, 999_999_999);
    }

    private BudgetResponse toResponse(Budget budget, boolean includeTransactions) {
        BigDecimal spent = transactionRepository.sumAmountForBudgetAndTypeInPeriod(
                budget.getId(),
                TransactionType.EXPENSE,
                atDayStart(budget.getStartDate()),
                atDayEnd(budget.getEndDate()));
        return budgetMapper.toResponse(budget, spent, includeTransactions);
    }
}
