package com.trackit.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackit.auth.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(
        statements = {
            "DELETE FROM goals",
            "DELETE FROM schedules",
            "DELETE FROM cash_entries",
            "DELETE FROM budget_categories",
            "DELETE FROM budget_plans",
            "DELETE FROM accounts",
            "DELETE FROM users WHERE id = 1",
            "INSERT INTO users(id, name, email, password) VALUES (1, 'u1', 'u1@test.com', 'x')",
            "INSERT INTO accounts(id, user_id, name, type, balance) VALUES (1, 1, 'a1', 'BANK', 100.00), (2, 1, 'a2', 'CASH', 50.00)",
            "INSERT INTO budget_plans(id, user_id, name, total_income, is_percentage, is_active) VALUES (1, 1, 'P', 10000.00, false, true)",
            "INSERT INTO budget_categories(id, plan_id, name, type, allocation_value) VALUES (1, 1, 'Food', 'NEED', 100.00)",
            "INSERT INTO budget_categories(id, plan_id, name, type, allocation_value) VALUES (2, 1, 'Fun', 'WANT', 20.00)",
            "INSERT INTO cash_entries(id, user_id, account_id, direction, title, amount, entry_date, is_recurring, category_id) VALUES (1, 1, 1, 'INCOME', 'old', 500.00, CURRENT_DATE - 45, false, null)",
            "INSERT INTO cash_entries(id, user_id, account_id, direction, title, amount, entry_date, is_recurring, category_id) VALUES (2, 1, 1, 'INCOME', 'mtd', 300.00, CURRENT_DATE, false, null)",
            "INSERT INTO cash_entries(id, user_id, account_id, direction, title, amount, entry_date, is_recurring, category_id) VALUES (3, 1, 1, 'EXPENSE', 'oldE', 100.00, CURRENT_DATE - 45, false, 1)",
            "INSERT INTO cash_entries(id, user_id, account_id, direction, title, amount, entry_date, is_recurring, category_id) VALUES (4, 1, 1, 'EXPENSE', 'mtdE', 40.00, CURRENT_DATE, false, 1)",
            "INSERT INTO cash_entries(id, user_id, account_id, direction, title, amount, entry_date, is_recurring, category_id) VALUES (5, 1, 1, 'EXPENSE', 'fun', 50.00, CURRENT_DATE, false, 2)",
            "INSERT INTO schedules(id, user_id, title, type, schedule_date, status) VALUES (10, 1, 't1', 'TASK', CURRENT_DATE, 'PENDING')",
            "INSERT INTO schedules(id, user_id, title, type, schedule_date, status) VALUES (11, 1, 't2', 'TASK', CURRENT_DATE, 'DONE')",
            "INSERT INTO schedules(id, user_id, title, type, schedule_date, status) VALUES (12, 1, 'soon', 'TASK', CURRENT_DATE + 3, 'PENDING')",
            "INSERT INTO schedules(id, user_id, title, type, schedule_date, status) VALUES (13, 1, 'late', 'TASK', CURRENT_DATE - 2, 'PENDING')"
        })
class DashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void dashboard_returnsAggregates() throws Exception {
        LocalDate today = LocalDate.now();
        int daysRemaining = (int) ChronoUnit.DAYS.between(today, today.withDayOfMonth(today.lengthOfMonth())) + 1;
        BigDecimal expectedTodayLimit =
                new BigDecimal("30.00").divide(BigDecimal.valueOf(daysRemaining), 2, RoundingMode.HALF_UP);

        MvcResult result =
                mockMvc.perform(get("/api/dashboard").header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cash.totalIncome").value(800.0))
                .andExpect(jsonPath("$.cash.totalExpense").value(190.0))
                .andExpect(jsonPath("$.cash.net").value(610.0))
                .andExpect(jsonPath("$.cashThisMonth.totalIncome").value(300.0))
                .andExpect(jsonPath("$.cashThisMonth.totalExpense").value(90.0))
                .andExpect(jsonPath("$.cashThisMonth.net").value(210.0))
                .andExpect(jsonPath("$.expenses.entryCount").value(3))
                .andExpect(jsonPath("$.accounts.accountCount").value(2))
                .andExpect(jsonPath("$.accounts.totalBalance").value(150.0))
                .andExpect(jsonPath("$.ledger.totalEntries").value(0))
                .andExpect(jsonPath("$.ledger.pendingEntries").value(0))
                .andExpect(jsonPath("$.ledger.settledEntries").value(0))
                .andExpect(jsonPath("$.schedule.todayTotal").value(2))
                .andExpect(jsonPath("$.schedule.todayPending").value(1))
                .andExpect(jsonPath("$.schedule.todayDone").value(1))
                .andExpect(jsonPath("$.schedule.pendingNextSevenDays").value(2))
                .andExpect(jsonPath("$.schedule.overduePending").value(1))
                .andExpect(jsonPath("$.budget.planId").value(1))
                .andExpect(jsonPath("$.budget.categories[0].categoryId").exists())
                .andExpect(jsonPath("$.budgetSummary.planId").value(1))
                .andExpect(jsonPath("$.budgetSummary.totalAllocated").value(120.0))
                .andExpect(jsonPath("$.budgetSummary.totalSpentThisMonth").value(90.0))
                .andExpect(jsonPath("$.budgetSummary.remainingThisMonth").value(30.0))
                .andExpect(jsonPath("$.budgetSummary.utilizationPercent").value(75.0))
                .andExpect(jsonPath("$.budgetSummary.daysRemainingInMonth").value(daysRemaining))
                .andExpect(jsonPath("$.overBudgetCategories[0].categoryId").value(2))
                .andExpect(jsonPath("$.overBudgetCategories[0].overAmount").value(30.0))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        BigDecimal limit = new BigDecimal(root.path("budgetSummary").path("todaySpendingLimit").asText());
        assertThat(limit).isEqualByComparingTo(expectedTodayLimit);
    }
}
