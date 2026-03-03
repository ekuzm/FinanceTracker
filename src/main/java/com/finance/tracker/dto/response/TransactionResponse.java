package com.finance.tracker.dto.response;

import com.finance.tracker.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private Long id;
    private LocalDateTime occurredAt;
    private BigDecimal amount;
    private String description;
    private TransactionType type;
    private Long budgetId;
    private String budgetName;
    private Long accountId;
    private String accountName;
    private Long userId;
    private String username;
    private List<Long> tagIds;
    private UUID transferId;
}
