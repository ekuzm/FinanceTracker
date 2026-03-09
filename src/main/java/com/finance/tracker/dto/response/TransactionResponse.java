package com.finance.tracker.dto.response;

import com.finance.tracker.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    private Long accountId;
    private String accountName;
    private List<Long> tagIds;
}
