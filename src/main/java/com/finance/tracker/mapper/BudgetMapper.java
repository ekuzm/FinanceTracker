package com.finance.tracker.mapper;

import com.finance.tracker.domain.Budget;
import com.finance.tracker.domain.Transaction;
import com.finance.tracker.domain.User;
import com.finance.tracker.dto.request.BudgetRequest;
import com.finance.tracker.dto.response.BudgetResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BudgetMapper {

    public BudgetResponse toResponse(Budget budget, BigDecimal spent, boolean includeTransactions) {
        if (budget == null) {
            return null;
        }

        BigDecimal safeSpent = spent == null ? BigDecimal.ZERO : spent;
        BigDecimal remainingAmount = budget.getLimitAmount().subtract(safeSpent);

        BudgetResponse response = new BudgetResponse();
        response.setId(budget.getId());
        response.setName(budget.getName());
        response.setLimitAmount(budget.getLimitAmount());
        response.setStartDate(budget.getStartDate());
        response.setEndDate(budget.getEndDate());
        response.setUserId(budget.getUser() != null ? budget.getUser().getId() : null);
        response.setSpent(safeSpent);
        response.setRemainingAmount(remainingAmount);
        response.setOverLimit(remainingAmount.signum() < 0);

        if (includeTransactions) {
            response.setTransactionIds(
                    budget.getTransactions() != null
                            ? budget.getTransactions().stream().map(Transaction::getId).toList()
                            : null);
        } else {
            response.setTransactionIds(null);
        }

        return response;
    }

    public Budget fromRequest(BudgetRequest request, User user) {
        if (request == null) {
            return null;
        }

        Budget budget = new Budget();
        budget.setName(request.getName());
        budget.setLimitAmount(request.getLimitAmount());
        budget.setStartDate(request.getStartDate());
        budget.setEndDate(request.getEndDate());
        budget.setUser(user);

        return budget;
    }

    public BudgetRequest toRequest(Budget budget) {
        if (budget == null) {
            return null;
        }

        BudgetRequest request = new BudgetRequest();
        request.setName(budget.getName());
        request.setLimitAmount(budget.getLimitAmount());
        request.setStartDate(budget.getStartDate());
        request.setEndDate(budget.getEndDate());
        request.setUserId(budget.getUser() != null ? budget.getUser().getId() : null);

        return request;
    }
}
