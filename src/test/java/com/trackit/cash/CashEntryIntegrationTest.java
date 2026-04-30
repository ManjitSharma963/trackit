package com.trackit.cash;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(statements = {
        "DELETE FROM goals",
        "DELETE FROM cash_entries",
        "DELETE FROM budget_categories",
        "DELETE FROM budget_plans",
        "DELETE FROM accounts",
        "DELETE FROM users",
        "INSERT INTO users(id, name, email, password) VALUES (1, 'u1', 'u1@test.com', 'x')",
        "INSERT INTO accounts(id, user_id, name, type, balance) VALUES (1, 1, 'main', 'BANK', 0)",
        "INSERT INTO budget_plans(id, user_id, name, total_income, is_percentage, is_active) VALUES (1, 1, 'Plan', 5000.00, false, true)",
        "INSERT INTO budget_categories(id, plan_id, name, type, allocation_value) VALUES (1, 1, 'Groceries', 'NEED', 500.00)"
})
class CashEntryIntegrationTest {

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
    void createListSummaryUpdateDeleteFlow() throws Exception {
        CashEntryRequest income = request(CashDirection.INCOME, new BigDecimal("100.00"), "Salary");
        CashEntryRequest expense = request(CashDirection.EXPENSE, new BigDecimal("40.00"), "Food");

        mockMvc.perform(post("/api/cash-entries")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(income)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.direction").value("INCOME"))
                .andExpect(jsonPath("$.category").value("Salary"));

        MvcResult expenseResult = mockMvc.perform(post("/api/cash-entries")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expense)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.direction").value("EXPENSE"))
                .andExpect(jsonPath("$.category").value("Food"))
                .andExpect(jsonPath("$.categoryId").value(1))
                .andReturn();
        long expenseId = objectMapper.readTree(expenseResult.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(get("/api/cash-entries")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1))
                        .param("direction", "INCOME")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].category").value("Salary"));

        mockMvc.perform(get("/api/cash-entries/summary").header(HttpHeaders.AUTHORIZATION, bearer(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(100.00))
                .andExpect(jsonPath("$.totalExpense").value(40.00))
                .andExpect(jsonPath("$.net").value(60.00));

        CashEntryRequest updated = request(CashDirection.EXPENSE, new BigDecimal("45.00"), "Groceries");
        mockMvc.perform(put("/api/cash-entries/{id}", expenseId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(45.00))
                .andExpect(jsonPath("$.category").value("Groceries"));

        mockMvc.perform(delete("/api/cash-entries/{id}", expenseId).header(HttpHeaders.AUTHORIZATION, bearer(1)))
                .andExpect(status().isNoContent());
    }

    @Test
    void expenseWithoutCategoryId_isAllowed_labelOnly() throws Exception {
        CashEntryRequest expense = request(CashDirection.EXPENSE, new BigDecimal("10.00"), "Food");
        expense.setCategoryId(null);

        mockMvc.perform(post("/api/cash-entries")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expense)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("Food"));
    }

    @Test
    void createExpenseWithoutAccountId_usesDefaultAccount() throws Exception {
        CashEntryRequest expense = request(CashDirection.EXPENSE, new BigDecimal("12.00"), "Coffee");
        expense.setAccountId(null);

        mockMvc.perform(post("/api/cash-entries")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expense)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(1));
    }

    @Test
    void expenseWithUnknownCategoryId_returnsNotFound() throws Exception {
        CashEntryRequest expense = request(CashDirection.EXPENSE, new BigDecimal("10.00"), "Food");
        expense.setCategoryId(99999L);

        mockMvc.perform(post("/api/cash-entries")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expense)))
                .andExpect(status().isNotFound());
    }

    @Test
    void incomeWithCategoryId_returnsBadRequest() throws Exception {
        CashEntryRequest income = request(CashDirection.INCOME, new BigDecimal("50.00"), "Salary");
        income.setCategoryId(1L);

        mockMvc.perform(post("/api/cash-entries")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(income)))
                .andExpect(status().isBadRequest());
    }

    private CashEntryRequest request(CashDirection direction, BigDecimal amount, String category) {
        CashEntryRequest request = new CashEntryRequest();
        request.setUserId(1L);
        request.setAccountId(1L);
        request.setDirection(direction);
        request.setTitle(direction.name());
        request.setAmount(amount);
        request.setCategory(category);
        request.setEntryDate(LocalDate.of(2026, 4, 21));
        request.setRecurring(false);
        if (direction == CashDirection.EXPENSE) {
            request.setCategoryId(1L);
        }
        return request;
    }
}
