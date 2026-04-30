package com.trackit.budget.dto;

import com.trackit.budget.model.BudgetCategoryType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CreateCategoryRequest {

    @NotNull
    private Long planId;

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    private BudgetCategoryType type;

    @NotNull
    @DecimalMin(value = "0.01", message = "must be at least 0.01")
    private BigDecimal value;

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BudgetCategoryType getType() {
        return type;
    }

    public void setType(BudgetCategoryType type) {
        this.type = type;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }
}
