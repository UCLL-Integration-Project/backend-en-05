package be.ucll.it.courses.backend.controller;

import be.ucll.it.courses.backend.config.SecurityConfig;
import be.ucll.it.courses.backend.model.User;
import be.ucll.it.courses.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommandController.class)
@Import(SecurityConfig.class)
public class CommandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserRepository userRepository;

    private final String VALID_TOKEN = "valid-admin-token";
    private final String INVALID_TOKEN = "invalid-token";

    @BeforeEach
    void setUp() {
        User admin = new User("admin", "Admin", "User", VALID_TOKEN, "password");
        when(userRepository.findByToken(VALID_TOKEN)).thenReturn(Optional.of(admin));
        when(userRepository.findByToken(INVALID_TOKEN)).thenReturn(Optional.empty());
    }

    @Test
    void sendCommand_ValidAdmin_ReturnsAccepted() throws Exception {
        Map<String, Object> body = Map.of(
                "action", "move_forward",
                "duration_ms", 1000
        );

        mockMvc.perform(post("/command")
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .header("X-Role", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.command_id").exists())
                .andExpect(jsonPath("$.status").value("dispatched"));
    }

    @Test
    void sendCommand_InvalidToken_ReturnsUnauthorized() throws Exception {
        Map<String, Object> body = Map.of("action", "stop");

        mockMvc.perform(post("/command")
                        .header("Authorization", "Bearer " + INVALID_TOKEN)
                        .header("X-Role", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sendCommand_MissingToken_ReturnsUnauthorized() throws Exception {
        Map<String, Object> body = Map.of("action", "stop");

        mockMvc.perform(post("/command")
                        .header("X-Role", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sendCommand_NotAdmin_ReturnsForbidden() throws Exception {
        Map<String, Object> body = Map.of("action", "stop");

        mockMvc.perform(post("/command")
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .header("X-Role", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    void sendCommand_InvalidAction_ReturnsUnprocessableEntity() throws Exception {
        Map<String, Object> body = Map.of("action", "invalid_action");

        mockMvc.perform(post("/command")
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .header("X-Role", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void sendCommand_MissingAction_ReturnsBadRequest() throws Exception {
        Map<String, Object> body = Map.of("duration_ms", 500);

        mockMvc.perform(post("/command")
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .header("X-Role", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}
