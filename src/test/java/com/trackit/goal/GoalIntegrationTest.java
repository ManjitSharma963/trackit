package com.trackit.goal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackit.auth.service.JwtService;
import com.trackit.goals.dto.CreateGoalRequest;
import com.trackit.goals.dto.UpdateGoalProgressRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
            "DELETE FROM ledger",
            "DELETE FROM accounts",
            "DELETE FROM users",
            "INSERT INTO users(id, name, email, password) VALUES (1, 'u1', 'u1@test.com', 'x')"
        })
class GoalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Test
    void createListUpdateProgressFlow() throws Exception {
        CreateGoalRequest create = new CreateGoalRequest();
        create.setTitle("Emergency fund");
        create.setDescription("6 months expenses");
        create.setTargetAmount(new BigDecimal("10000.00"));
        create.setTargetDate(LocalDate.of(2026, 12, 31));

        MvcResult created =
                mockMvc.perform(post("/api/goals")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(1L))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(create)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.title").value("Emergency fund"))
                        .andExpect(jsonPath("$.targetAmount").value(10000.0))
                        .andExpect(jsonPath("$.currentAmount").value(0))
                        .andExpect(jsonPath("$.progressPercent").value(0))
                        .andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/goals").header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(id));

        UpdateGoalProgressRequest progress = new UpdateGoalProgressRequest();
        progress.setCurrentAmount(new BigDecimal("2500.50"));

        mockMvc.perform(put("/api/goals/{id}/progress", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(progress)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentAmount").value(2500.5))
                .andExpect(jsonPath("$.progressPercent").value(25.01));
    }
}
