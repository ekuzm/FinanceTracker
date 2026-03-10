package com.finance.tracker.dto.request;

import com.finance.tracker.domain.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
@Schema(description = "Request body for partially updating a transaction.")
public class TransactionUpdateRequest {

    private LocalDateTime occurredAt;

    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    @NotBlank(message = "must not be blank")
    @Size(min = 3, max = 255)
    private String description;

    private TransactionType type;

    @Positive
    private Long accountId;

    private List<Long> tagIds;
}
