package com.trackit.ledger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackit.auth.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(
        statements = {
            "DELETE FROM ledger",
            "DELETE FROM goals",
            "DELETE FROM cash_entries",
            "DELETE FROM budget_categories",
            "DELETE FROM budget_plans",
            "DELETE FROM accounts",
            "DELETE FROM users",
            "INSERT INTO users(id, name, email, password) VALUES (1, 'u1', 'u1@test.com', 'x')",
            "INSERT INTO accounts(id, user_id, name, type, balance) VALUES (1, 1, 'main', 'BANK', 1000.00)"
        })
class LedgerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Test
    void settledGive_decreasesAccount_takeIncreases_reopenReverses() throws Exception {
        MvcResult pendingRes =
                mockMvc.perform(post("/api/ledger")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(1L))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"userId":1,"personName":"Alex","type":"GIVE","amount":200.00,"transactionDate":"2026-04-20","status":"PENDING"}
                                        """
                                                .trim()))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.status").value("PENDING"))
                        .andReturn();
        long giveId = objectMapper.readTree(pendingRes.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/accounts").header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].balance").value(1000.0));

        mockMvc.perform(put("/api/ledger/{id}", giveId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"userId":1,"personName":"Alex","type":"GIVE","amount":200.00,"transactionDate":"2026-04-20","status":"SETTLED","accountId":1}
                                """
                                        .trim()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SETTLED"));

        mockMvc.perform(get("/api/accounts").header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].balance").value(800.0));

        mockMvc.perform(post("/api/ledger")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"userId":1,"personName":"Sam","type":"TAKE","amount":150.00,"transactionDate":"2026-04-21","status":"SETTLED","accountId":1}
                                """
                                        .trim()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SETTLED"));

        mockMvc.perform(get("/api/accounts").header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].balance").value(950.0));

        mockMvc.perform(put("/api/ledger/{id}", giveId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"userId":1,"personName":"Alex","type":"GIVE","amount":200.00,"transactionDate":"2026-04-20","status":"PENDING","accountId":1}
                                """
                                        .trim()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/accounts").header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].balance").value(1150.0));
    }

    @Test
    void settledWithoutAccount_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/ledger")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"userId":1,"personName":"X","type":"GIVE","amount":10.00,"transactionDate":"2026-04-22","status":"SETTLED"}
                                """
                                        .trim()))
                .andExpect(status().isBadRequest());
    }
}
