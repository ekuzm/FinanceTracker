package com.finance.tracker.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for transferring money between accounts.")
public class AccountTransferRequest {

    @NotNull
    @Positive
    private Long fromAccountId;

    @NotNull
    @Positive
    private Long toAccountId;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    private LocalDateTime occurredAt;

    @Size(max = 255)
    private String note;
}
