package com.trackit.budget;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackit.auth.service.JwtService;
import com.trackit.cash.dto.CashEntryRequest;
import com.trackit.cash.model.CashDirection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(
        statements = {
            "DELETE FROM goals",
            "DELETE FROM cash_entries",
            "DELETE FROM budget_categories",
            "DELETE FROM budget_plans",
            "DELETE FROM accounts",
            "DELETE FROM users",
            "INSERT INTO users(id, name, email, password) VALUES (1, 'u1', 'u1@test.com', 'x')",
            "INSERT INTO accounts(id, user_id, name, type, balance) VALUES (1, 1, 'main', 'BANK', 0)"
        })
class BudgetIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    private String bearer(long userId) {
        return "Bearer " + jwtService.createAccessToken(userId);
    }

    @Test
    void planCategoriesStatusAndCashExpenseLink() throws Exception {
        String planBody =
                """
                {"name":"April","totalIncome":10000.00,"isPercentage":true,"isActive":true}
                """;
        MvcResult planResult = mockMvc.perform(post("/api/budget/plans")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(planBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isActive").value(true))
                .andReturn();
        long planId = objectMapper.readTree(planResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/budget/categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"planId":%d,"name":"Food","type":"NEED","value":40.00}
                                """
                                        .formatted(planId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/budget/categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"planId":%d,"name":"Bills","type":"NEED","value":70.00}
                                """
                                        .formatted(planId)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/budget/categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"planId":%d,"name":"Bills","type":"NEED","value":60.00}
                                """
                                        .formatted(planId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/budget/status").header(HttpHeaders.AUTHORIZATION, bearer(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(10000.00))
                .andExpect(jsonPath("$.categories[0].spentAmount").exists());

        MvcResult cats = mockMvc.perform(get("/api/budget/categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1))
                        .param("planId", String.valueOf(planId)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode arr = objectMapper.readTree(cats.getResponse().getContentAsString());
        long foodCategoryId = arr.get(0).get("id").asLong();

        CashEntryRequest expense = new CashEntryRequest();
        expense.setAccountId(1L);
        expense.setDirection(CashDirection.EXPENSE);
        expense.setTitle("Groceries");
        expense.setAmount(new BigDecimal("150.00"));
        expense.setEntryDate(LocalDate.of(2026, 4, 24));
        expense.setRecurring(false);
        expense.setCategoryId(foodCategoryId);

        mockMvc.perform(post("/api/cash-entries")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expense)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryId").value((int) foodCategoryId));

        mockMvc.perform(get("/api/budget/status").header(HttpHeaders.AUTHORIZATION, bearer(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[0].spentAmount").value(150.00));
    }

    @Test
    void statusWithoutActivePlanReturns400() throws Exception {
        mockMvc.perform(get("/api/budget/status").header(HttpHeaders.AUTHORIZATION, bearer(1)))
                .andExpect(status().isBadRequest());
    }
}
