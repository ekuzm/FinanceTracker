package com.finance.tracker.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Budget response payload.")
public class BudgetResponse {

    private Long id;
    private String name;
    private BigDecimal limitAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long userId;
}
