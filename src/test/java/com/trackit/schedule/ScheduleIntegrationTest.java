package com.trackit.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackit.auth.service.JwtService;
import com.trackit.schedule.dto.ScheduleRequest;
import com.trackit.schedule.model.ScheduleRepeatType;
import com.trackit.schedule.model.ScheduleType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
            "DELETE FROM accounts",
            "DELETE FROM users WHERE id = 1",
            "INSERT INTO users(id, name, email, password) VALUES (1, 'u1', 'u1@test.com', 'x')"
        })
class ScheduleIntegrationTest {

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
    void createListUpdateDoneDeleteFlow() throws Exception {
        ScheduleRequest timed = request("Morning Task", LocalTime.of(9, 30));
        ScheduleRequest noTime = request("No Time Task", null);

        MvcResult timedResult =         mockMvc.perform(post("/api/schedules")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(timed)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        long timedId = objectMapper.readTree(timedResult.getResponse().getContentAsString()).get("id").asLong();

        MvcResult noTimeResult = mockMvc.perform(post("/api/schedules")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noTime)))
                .andExpect(status().isCreated())
                .andReturn();
        long noTimeId = objectMapper.readTree(noTimeResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/schedules")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1))
                        .param("date", "2026-04-21")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("Morning Task"))
                .andExpect(jsonPath("$.items[1].title").value("No Time Task"));

        ScheduleRequest updated = request("Morning Task Updated", LocalTime.of(10, 0));
        mockMvc.perform(put("/api/schedules/{id}", timedId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Morning Task Updated"));

        mockMvc.perform(put("/api/schedules/{id}/done", timedId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));

        mockMvc.perform(delete("/api/schedules/{id}", noTimeId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(1)))
                .andExpect(status().isNoContent());
    }

    private ScheduleRequest request(String title, LocalTime scheduleTime) {
        ScheduleRequest request = new ScheduleRequest();
        request.setTitle(title);
        request.setType(ScheduleType.TASK);
        request.setScheduleDate(LocalDate.of(2026, 4, 21));
        request.setScheduleTime(scheduleTime);
        request.setNotes("n");
        request.setReminderMinutes(10);
        request.setRepeatType(ScheduleRepeatType.NONE);
        return request;
    }
}
