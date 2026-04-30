package com.trackit.budget.dto;

import com.trackit.budget.model.BudgetCategoryType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BudgetCategoryResponse {

    private Long id;
    private Long planId;
    private String name;
    private BudgetCategoryType type;
    private BigDecimal value;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
