package br.com.vivovaloriza.dbmetricsmonitor.controller;

import br.com.vivovaloriza.dbmetricsmonitor.dto.HealthResponse;
import br.com.vivovaloriza.dbmetricsmonitor.exception.GlobalExceptionHandler;
import br.com.vivovaloriza.dbmetricsmonitor.service.HealthService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HealthControllerTest {

    @Test
    void shouldReturnApplicationAndDatabaseHealth() throws Exception {
        HealthService healthService = mock(HealthService.class);
        when(healthService.health()).thenReturn(new HealthResponse("UP", "UP", Instant.parse("2026-03-23T12:00:00Z")));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HealthController(healthService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.databaseStatus").value("UP"));
    }
}
