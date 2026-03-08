package com.finance.tracker.mapper;

import com.finance.tracker.domain.Transaction;
import com.finance.tracker.domain.TransactionType;
import com.finance.tracker.domain.Transfer;
import com.finance.tracker.dto.response.TransferResponse;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class TransferMapper {

    public TransferResponse toResponse(Transfer transfer) {
        if (transfer == null) {
            return null;
        }

        Transaction expense = findByType(transfer, TransactionType.EXPENSE);
        Transaction income = findByType(transfer, TransactionType.INCOME);

        BigDecimal amount = resolveAmount(expense, income);
        LocalDateTime occurredAt = resolveOccurredAt(expense, income);

        TransferResponse response = new TransferResponse();
        response.setId(transfer.getId());
        response.setFromAccountId(
                expense != null && expense.getAccount() != null ? expense.getAccount().getId() : null);
        response.setToAccountId(income != null && income.getAccount() != null ? income.getAccount().getId() : null);
        response.setAmount(amount);
        response.setOccurredAt(occurredAt);
        response.setNote(transfer.getNote());

        return response;
    }

    private Transaction findByType(Transfer transfer, TransactionType type) {
        if (transfer.getTransactions() == null) {
            return null;
        }
        return transfer.getTransactions().stream()
                .filter(transaction -> transaction.getType() == type)
                .findFirst()
                .orElse(null);
    }

    private BigDecimal resolveAmount(Transaction expense, Transaction income) {
        if (expense != null) {
            return expense.getAmount();
        }
        if (income != null) {
            return income.getAmount();
        }
        return null;
    }

    private LocalDateTime resolveOccurredAt(Transaction expense, Transaction income) {
        if (expense != null) {
            return expense.getOccurredAt();
        }
        if (income != null) {
            return income.getOccurredAt();
        }
        return null;
    }
}
