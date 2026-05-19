package be.ucll.it.courses.backend.integration.http;

import be.ucll.it.courses.backend.integration.BaseIntegrationTest;
import be.ucll.it.courses.backend.model.User;
import be.ucll.it.courses.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CommandIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    private String adminToken;
    private String viewerToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        
        adminToken = UUID.randomUUID().toString();
        User admin = new User();
        admin.setUsername("admin");
        admin.setToken(adminToken);
        admin.setRole("ADMIN");
        userRepository.save(admin);

        viewerToken = UUID.randomUUID().toString();
        User viewer = new User();
        viewer.setUsername("viewer");
        viewer.setToken(viewerToken);
        viewer.setRole("VIEWER");
        userRepository.save(viewer);
    }

    @Test
    void postCommandReturns202ForAdmin() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.set("X-Role", "admin");
        
        Map<String, Object> body = Map.of(
            "action", "move_forward",
            "duration_ms", 1000
        );
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity("/v1/command", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    void postCommandReturns403ForViewer() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(viewerToken);
        headers.set("X-Role", "viewer");
        
        Map<String, Object> body = Map.of(
            "action", "move_forward"
        );
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity("/v1/command", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void postCommandReturns422ForUnknownAction() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.set("X-Role", "admin");
        
        Map<String, Object> body = Map.of(
            "action", "invalid_action"
        );
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity("/v1/command", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
