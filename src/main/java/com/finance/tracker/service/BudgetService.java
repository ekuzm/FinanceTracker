package com.finance.tracker.service;

import com.finance.tracker.dto.request.BudgetRequest;
import com.finance.tracker.dto.request.BudgetUpdateRequest;
import com.finance.tracker.dto.response.BudgetResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BudgetService {
    BudgetResponse getBudgetById(Long id);

    Page<BudgetResponse> getAllBudgets(Pageable pageable);

    BudgetResponse createBudget(BudgetRequest request);

    BudgetResponse updateBudget(Long id, BudgetUpdateRequest request);

    void deleteBudget(Long id);
}
