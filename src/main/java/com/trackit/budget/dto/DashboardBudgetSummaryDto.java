package com.trackit.budget.dto;

import java.math.BigDecimal;

/** Month-to-date rollup for the active plan (aligned with calendar month). */
public class DashboardBudgetSummaryDto {

    private Long planId;
    private String planName;
    private BigDecimal totalIncome;
    /** Sum of per-category monthly allocations. */
    private BigDecimal totalAllocated;
    /** Sum of expenses this month in plan categories. */
    private BigDecimal totalSpentThisMonth;
    private BigDecimal remainingThisMonth;
    /** {@code totalSpentThisMonth / totalAllocated * 100} when {@code totalAllocated > 0}; otherwise null. */
    private BigDecimal utilizationPercent;
    private int daysRemainingInMonth;
    /**
     * Suggested cap for discretionary spending today: remaining plan budget for the month spread over days left
     * (including today). Zero when already at or over the monthly allocation.
     */
    private BigDecimal todaySpendingLimit;

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

    public BigDecimal getTotalAllocated() {
        return totalAllocated;
    }

    public void setTotalAllocated(BigDecimal totalAllocated) {
        this.totalAllocated = totalAllocated;
    }

    public BigDecimal getTotalSpentThisMonth() {
        return totalSpentThisMonth;
    }

    public void setTotalSpentThisMonth(BigDecimal totalSpentThisMonth) {
        this.totalSpentThisMonth = totalSpentThisMonth;
    }

    public BigDecimal getRemainingThisMonth() {
        return remainingThisMonth;
    }

    public void setRemainingThisMonth(BigDecimal remainingThisMonth) {
        this.remainingThisMonth = remainingThisMonth;
    }

    public BigDecimal getUtilizationPercent() {
        return utilizationPercent;
    }

    public void setUtilizationPercent(BigDecimal utilizationPercent) {
        this.utilizationPercent = utilizationPercent;
    }

    public int getDaysRemainingInMonth() {
        return daysRemainingInMonth;
    }

    public void setDaysRemainingInMonth(int daysRemainingInMonth) {
        this.daysRemainingInMonth = daysRemainingInMonth;
    }

    public BigDecimal getTodaySpendingLimit() {
        return todaySpendingLimit;
    }

    public void setTodaySpendingLimit(BigDecimal todaySpendingLimit) {
        this.todaySpendingLimit = todaySpendingLimit;
    }
}
