package com.trackit.budget.dto;

import com.trackit.budget.model.BudgetCategoryType;

import java.math.BigDecimal;

/** Category where month-to-date spend exceeds its monthly allocation. */
public class DashboardOverBudgetCategoryDto {

    private Long categoryId;
    private String name;
    private BudgetCategoryType type;
    private BigDecimal allocatedAmount;
    private BigDecimal spentThisMonth;
    /** Positive: {@code spentThisMonth - allocatedAmount}. */
    private BigDecimal overAmount;

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
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

    public BigDecimal getAllocatedAmount() {
        return allocatedAmount;
    }

    public void setAllocatedAmount(BigDecimal allocatedAmount) {
        this.allocatedAmount = allocatedAmount;
    }

    public BigDecimal getSpentThisMonth() {
        return spentThisMonth;
    }

    public void setSpentThisMonth(BigDecimal spentThisMonth) {
        this.spentThisMonth = spentThisMonth;
    }

    public BigDecimal getOverAmount() {
        return overAmount;
    }

    public void setOverAmount(BigDecimal overAmount) {
        this.overAmount = overAmount;
    }
}
