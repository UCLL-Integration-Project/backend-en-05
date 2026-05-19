package be.ucll.it.courses.backend.integration.http;

import be.ucll.it.courses.backend.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

public class HealthIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void healthReturns200WithCorrectSchema() {
        ResponseEntity<String> response = restTemplate.getForEntity("/v1/health", String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"ok\"");
        assertThat(response.getBody()).contains("\"db\":\"connected\"");
    }
}
