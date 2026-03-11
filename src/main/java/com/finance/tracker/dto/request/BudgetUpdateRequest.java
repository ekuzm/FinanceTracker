package com.finance.tracker.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for partially updating a budget.")
public class BudgetUpdateRequest {

    @Pattern(regexp = ".*\\S.*", message = "must not be blank")
    @Size(min = 3, max = 50)
    private String name;

    @Positive
    private BigDecimal limitAmount;

    private LocalDate startDate;

    private LocalDate endDate;

    @Min(1)
    private Long userId;
}
