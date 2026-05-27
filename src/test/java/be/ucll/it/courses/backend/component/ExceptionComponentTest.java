package be.ucll.it.courses.backend.component;

import be.ucll.it.courses.backend.exception.ResourceNotFoundException;
import be.ucll.it.courses.backend.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ExceptionComponentTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testUnauthorizedExceptionMapping() throws Exception {
        // Calling POST /v1/events with a VALID body but NO headers should trigger UnauthorizedException
        String validBody = "{\"timestamp\": \"2026-05-27T10:00:00Z\", \"temperature\": 25.0, \"battery_pct\": 100, \"event_type\": \"fire\"}";
        
        mockMvc.perform(post("/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid or missing Authorization header"));
    }

    @Test
    @WithMockUser
    void testResourceNotFoundExceptionMapping() throws Exception {
        // Calling GET /v1/status when no robot has sent a heartbeat (lastSeen is null)
        // should trigger ResourceNotFoundException
        mockMvc.perform(get("/v1/status"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No robot status found. Has any robot connected yet?"));
    }
}
