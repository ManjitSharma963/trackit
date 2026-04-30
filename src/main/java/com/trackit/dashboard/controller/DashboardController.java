package com.trackit.dashboard.controller;

import com.trackit.dashboard.dto.DashboardResponse;
import com.trackit.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@PreAuthorize("hasRole('USER')")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(
            summary = "Dashboard",
            description =
                    "Aggregated snapshot: all-time and month-to-date cash, expense entry count, ledger counts, schedule rollups, account balances. When an active budget plan exists: `budget` (per-category status, all-time spend), `budgetSummary` (MTD totals, utilization %, days left in month, `todaySpendingLimit` = remaining allocation spread over those days), and `overBudgetCategories` (lines where MTD spend exceeds monthly allocation).")
    public DashboardResponse getDashboard() {
        return dashboardService.getDashboard();
    }
}
