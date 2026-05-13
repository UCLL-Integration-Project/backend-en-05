package be.ucll.it.courses.backend.controller;

import be.ucll.it.courses.backend.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthController.class)
@Import(SecurityConfig.class)
public class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JdbcTemplate jdbcTemplate;

    @Test
    void health_WhenDbConnected_ReturnsOk() throws Exception {
        mockMvc.perform(get("/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.db").value("connected"))
                .andExpect(jsonPath("$.uptime_s").isNumber());
    }

    @Test
    void health_WhenDbUnreachable_ReturnsServiceUnavailable() throws Exception {
        doThrow(new RuntimeException("DB down")).when(jdbcTemplate).execute(anyString());

        mockMvc.perform(get("/v1/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("degraded"))
                .andExpect(jsonPath("$.db").value("unreachable"));
    }
}
