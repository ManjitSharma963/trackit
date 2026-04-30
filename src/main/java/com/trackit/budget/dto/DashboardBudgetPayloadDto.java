package com.trackit.budget.dto;

import java.util.List;

/** Active-plan budget block for the dashboard (full status + MTD insights). */
public class DashboardBudgetPayloadDto {

    private BudgetStatusResponse status;
    private DashboardBudgetSummaryDto summary;
    private List<DashboardOverBudgetCategoryDto> overBudgetCategories;

    public BudgetStatusResponse getStatus() {
        return status;
    }

    public void setStatus(BudgetStatusResponse status) {
        this.status = status;
    }

    public DashboardBudgetSummaryDto getSummary() {
        return summary;
    }

    public void setSummary(DashboardBudgetSummaryDto summary) {
        this.summary = summary;
    }

    public List<DashboardOverBudgetCategoryDto> getOverBudgetCategories() {
        return overBudgetCategories;
    }

    public void setOverBudgetCategories(List<DashboardOverBudgetCategoryDto> overBudgetCategories) {
        this.overBudgetCategories = overBudgetCategories;
    }
}
