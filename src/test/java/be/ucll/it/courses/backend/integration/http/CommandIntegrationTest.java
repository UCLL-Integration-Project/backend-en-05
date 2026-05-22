package be.ucll.it.courses.backend.integration.http;

import be.ucll.it.courses.backend.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CommandIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void postCommandReturns202ForAdmin() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("ADMIN");

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
        headers.setBearerAuth("VIEWER");

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
        headers.setBearerAuth("ADMIN");

        Map<String, Object> body = Map.of(
            "action", "invalid_action"
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity("/v1/command", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void postCommandReturns401WithoutToken() {
        Map<String, Object> body = Map.of(
            "action", "move_forward"
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body);
        ResponseEntity<String> response = restTemplate.postForEntity("/v1/command", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
