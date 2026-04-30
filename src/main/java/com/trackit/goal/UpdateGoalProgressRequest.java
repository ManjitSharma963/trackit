package com.trackit.goals.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class UpdateGoalProgressRequest {

    @Schema(description = "Absolute progress toward the goal (same unit as targetAmount).")
    @NotNull
    @DecimalMin(value = "0.00", inclusive = true)
    private BigDecimal currentAmount;

    public BigDecimal getCurrentAmount() {
        return currentAmount;
    }

    public void setCurrentAmount(BigDecimal currentAmount) {
        this.currentAmount = currentAmount;
    }
}
