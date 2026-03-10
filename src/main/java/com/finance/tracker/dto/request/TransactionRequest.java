package com.finance.tracker.dto.request;

import com.finance.tracker.domain.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for creating a transaction.")
public class TransactionRequest {

    @NotNull
    private LocalDateTime occurredAt;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    @NotBlank(message = "must not be blank")
    @Size(min = 3, max = 255)
    private String description;

    @NotNull
    private TransactionType type;

    @NotNull
    @Positive
    private Long accountId;

    private List<Long> tagIds;
}
