package com.finance.tracker.mapper;

import com.finance.tracker.domain.Account;
import com.finance.tracker.domain.Budget;
import com.finance.tracker.domain.Tag;
import com.finance.tracker.domain.Transaction;
import com.finance.tracker.domain.User;
import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.response.TransactionResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TransactionMapper {

    public TransactionResponse toResponse(Transaction transaction) {
        return toResponse(transaction, true, true);
    }

    public TransactionResponse toResponse(Transaction transaction, boolean includeBudget, boolean includeUser) {
        if (transaction == null) {
            return null;
        }

        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());
        response.setOccurredAt(transaction.getOccurredAt());
        response.setAmount(transaction.getAmount());
        response.setDescription(transaction.getDescription());
        response.setType(transaction.getType());
        response.setTransferId(transaction.getTransferId());
        if (includeBudget) {
            response.setBudgetId(
                    transaction.getBudget() != null ? transaction.getBudget().getId() : null);
            response.setBudgetName(
                    transaction.getBudget() != null ? transaction.getBudget().getName() : null);
        }
        if (includeUser) {
            response.setUserId(
                    transaction.getUser() != null ? transaction.getUser().getId() : null);
            response.setUsername(
                    transaction.getUser() != null ? transaction.getUser().getUsername() : null);
        }
        response.setAccountId(transaction.getAccount() != null ? transaction.getAccount().getId() : null);
        response.setAccountName(transaction.getAccount() != null ? transaction.getAccount().getName() : null);
        response.setTagIds(
                transaction.getTags() != null ? transaction.getTags().stream().map(Tag::getId).toList() : List.of());

        return response;
    }

    public Transaction fromRequest(
            TransactionRequest request,
            User user,
            Account account,
            Budget budget,
            List<Tag> tags) {
        if (request == null) {
            return null;
        }

        Transaction transaction = new Transaction();
        transaction.setOccurredAt(request.getOccurredAt());
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setType(request.getType());
        transaction.setUser(user);
        transaction.setAccount(account);
        transaction.setBudget(budget);
        transaction.setTags(tags != null ? new ArrayList<>(tags) : new ArrayList<>());

        return transaction;
    }

    public TransactionRequest toRequest(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        TransactionRequest request = new TransactionRequest();
        request.setOccurredAt(transaction.getOccurredAt());
        request.setAmount(transaction.getAmount());
        request.setDescription(transaction.getDescription());
        request.setType(transaction.getType());
        request.setBudgetId(
                transaction.getBudget() != null ? transaction.getBudget().getId() : null);
        request.setAccountId(transaction.getAccount() != null ? transaction.getAccount().getId() : null);
        request.setUserId(transaction.getUser() != null ? transaction.getUser().getId() : null);
        request.setTagIds(
                transaction.getTags() != null ? transaction.getTags().stream().map(Tag::getId).toList() : null);

        return request;
    }
}
