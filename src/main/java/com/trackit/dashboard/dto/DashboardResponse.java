package com.trackit.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trackit.budget.dto.BudgetStatusResponse;
import com.trackit.budget.dto.DashboardBudgetSummaryDto;
import com.trackit.budget.dto.DashboardOverBudgetCategoryDto;
import com.trackit.cash.dto.CashEntrySummaryResponse;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardResponse {

    private CashEntrySummaryResponse cash;
    /** Income, expense, and net for the current calendar month (local date). */
    private CashEntrySummaryResponse cashThisMonth;
    private ExpensesSummaryDto expenses;
    private LedgerSummaryDto ledger;
    private ScheduleSummaryDto schedule;
    private AccountsSummaryDto accounts;
    /** Present when the user has an active budget plan; omitted otherwise. */
    private BudgetStatusResponse budget;
    /** Month-to-date budget rollup and suggested daily cap; omitted when no active plan. */
    private DashboardBudgetSummaryDto budgetSummary;
    /** Categories where MTD spend exceeds monthly allocation; omitted when no active plan. */
    private List<DashboardOverBudgetCategoryDto> overBudgetCategories;

    public DashboardResponse() {
    }

    public DashboardResponse(
            CashEntrySummaryResponse cash,
            CashEntrySummaryResponse cashThisMonth,
            ExpensesSummaryDto expenses,
            LedgerSummaryDto ledger,
            ScheduleSummaryDto schedule,
            AccountsSummaryDto accounts,
            BudgetStatusResponse budget,
            DashboardBudgetSummaryDto budgetSummary,
            List<DashboardOverBudgetCategoryDto> overBudgetCategories) {
        this.cash = cash;
        this.cashThisMonth = cashThisMonth;
        this.expenses = expenses;
        this.ledger = ledger;
        this.schedule = schedule;
        this.accounts = accounts;
        this.budget = budget;
        this.budgetSummary = budgetSummary;
        this.overBudgetCategories = overBudgetCategories;
    }

    public CashEntrySummaryResponse getCash() {
        return cash;
    }

    public void setCash(CashEntrySummaryResponse cash) {
        this.cash = cash;
    }

    public CashEntrySummaryResponse getCashThisMonth() {
        return cashThisMonth;
    }

    public void setCashThisMonth(CashEntrySummaryResponse cashThisMonth) {
        this.cashThisMonth = cashThisMonth;
    }

    public ExpensesSummaryDto getExpenses() {
        return expenses;
    }

    public void setExpenses(ExpensesSummaryDto expenses) {
        this.expenses = expenses;
    }

    public LedgerSummaryDto getLedger() {
        return ledger;
    }

    public void setLedger(LedgerSummaryDto ledger) {
        this.ledger = ledger;
    }

    public ScheduleSummaryDto getSchedule() {
        return schedule;
    }

    public void setSchedule(ScheduleSummaryDto schedule) {
        this.schedule = schedule;
    }

    public AccountsSummaryDto getAccounts() {
        return accounts;
    }

    public void setAccounts(AccountsSummaryDto accounts) {
        this.accounts = accounts;
    }

    public BudgetStatusResponse getBudget() {
        return budget;
    }

    public void setBudget(BudgetStatusResponse budget) {
        this.budget = budget;
    }

    public DashboardBudgetSummaryDto getBudgetSummary() {
        return budgetSummary;
    }

    public void setBudgetSummary(DashboardBudgetSummaryDto budgetSummary) {
        this.budgetSummary = budgetSummary;
    }

    public List<DashboardOverBudgetCategoryDto> getOverBudgetCategories() {
        return overBudgetCategories;
    }

    public void setOverBudgetCategories(List<DashboardOverBudgetCategoryDto> overBudgetCategories) {
        this.overBudgetCategories = overBudgetCategories;
    }
}
