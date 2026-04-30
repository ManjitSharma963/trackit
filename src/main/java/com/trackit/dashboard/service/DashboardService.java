package com.trackit.dashboard.service;

import com.trackit.auth.service.CurrentUserProvider;
import com.trackit.budget.dto.BudgetStatusResponse;
import com.trackit.budget.dto.DashboardBudgetSummaryDto;
import com.trackit.budget.dto.DashboardOverBudgetCategoryDto;
import com.trackit.budget.service.BudgetService;
import com.trackit.dashboard.dto.AccountsSummaryDto;
import com.trackit.dashboard.dto.DashboardResponse;
import com.trackit.dashboard.dto.ExpensesSummaryDto;
import com.trackit.dashboard.dto.LedgerSummaryDto;
import com.trackit.dashboard.dto.ScheduleSummaryDto;
import com.trackit.schedule.model.ScheduleStatus;
import com.trackit.schedule.repository.ScheduleRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class DashboardService {

    private final CurrentUserProvider currentUserProvider;
    private final ScheduleRepository scheduleRepository;
    private final BudgetService budgetService;
    private final CashSummaryService cashSummaryService;
    private final LedgerSummaryService ledgerSummaryService;
    private final AccountSummaryService accountSummaryService;

    public DashboardService(
            CurrentUserProvider currentUserProvider,
            ScheduleRepository scheduleRepository,
            BudgetService budgetService,
            CashSummaryService cashSummaryService,
            LedgerSummaryService ledgerSummaryService,
            AccountSummaryService accountSummaryService) {
        this.currentUserProvider = currentUserProvider;
        this.scheduleRepository = scheduleRepository;
        this.budgetService = budgetService;
        this.cashSummaryService = cashSummaryService;
        this.ledgerSummaryService = ledgerSummaryService;
        this.accountSummaryService = accountSummaryService;
    }

    public DashboardResponse getDashboard() {
        Long userId = currentUserProvider.getCurrentUserId();

        LocalDate today = LocalDate.now();

        var cashSummary = cashSummaryService.getCashAndExpenseSummary(userId, today);
        var cash = cashSummary.cash();
        var cashThisMonth = cashSummary.cashThisMonth();
        ExpensesSummaryDto expenses = cashSummary.expenses();

        LedgerSummaryDto ledger = ledgerSummaryService.getLedgerSummary(userId);

        long totalToday = scheduleRepository.countByUserIdAndScheduleDate(userId, today);
        long doneToday = scheduleRepository.countByUserIdAndScheduleDateAndStatus(userId, today, ScheduleStatus.DONE);
        long pendingToday =
                scheduleRepository.countByUserIdAndScheduleDateAndStatus(userId, today, ScheduleStatus.PENDING);
        LocalDate weekEnd = today.plusDays(6);
        long pendingNextSevenDays =
                scheduleRepository.countByUserIdAndScheduleDateBetweenAndStatus(
                        userId, today, weekEnd, ScheduleStatus.PENDING);
        long overduePending =
                scheduleRepository.countByUserIdAndScheduleDateLessThanAndStatus(
                        userId, today, ScheduleStatus.PENDING);
        ScheduleSummaryDto schedule =
                new ScheduleSummaryDto(
                        totalToday, pendingToday, doneToday, pendingNextSevenDays, overduePending);

        AccountsSummaryDto accounts = accountSummaryService.getAccountsSummary(userId);

        BudgetStatusResponse budget = null;
        DashboardBudgetSummaryDto budgetSummary = null;
        List<DashboardOverBudgetCategoryDto> overBudgetCategories = null;
        var budgetPayload = budgetService.getDashboardBudgetPayload();
        if (budgetPayload.isPresent()) {
            budget = budgetPayload.get().getStatus();
            budgetSummary = budgetPayload.get().getSummary();
            overBudgetCategories = budgetPayload.get().getOverBudgetCategories();
        }

        return new DashboardResponse(
                cash, cashThisMonth, expenses, ledger, schedule, accounts, budget, budgetSummary, overBudgetCategories);
    }

}
