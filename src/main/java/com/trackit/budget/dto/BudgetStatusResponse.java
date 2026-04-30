package com.trackit.budget.dto;

import java.math.BigDecimal;
import java.util.List;

public class BudgetStatusResponse {

    private Long planId;
    private String planName;
    private BigDecimal totalIncome;
    private List<BudgetCategoryStatusDto> categories;

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = totalIncome;
    }

    public List<BudgetCategoryStatusDto> getCategories() {
        return categories;
    }

    public void setCategories(List<BudgetCategoryStatusDto> categories) {
        this.categories = categories;
    }
}
