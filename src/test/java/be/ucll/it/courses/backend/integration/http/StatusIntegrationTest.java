package be.ucll.it.courses.backend.integration.http;

import be.ucll.it.courses.backend.integration.BaseIntegrationTest;
import be.ucll.it.courses.backend.model.Telemetry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class StatusIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private be.ucll.it.courses.backend.repository.EventRepository eventRepository;

    @Autowired
    private be.ucll.it.courses.backend.repository.TelemetryRepository telemetryRepository;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        telemetryRepository.deleteAll();
    }

    @Test
    void getStatusReturnsLatestRobotState() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("ADMIN");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange("/v1/status", HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.NOT_FOUND);

        if (response.getStatusCode() == HttpStatus.OK) {
            assertThat(response.getBody()).contains("mode");
            assertThat(response.getBody()).contains("battery_pct");
            assertThat(response.getBody()).contains("last_seen");
        }
    }

    @Test
    void getStatusReturns401WithoutToken() {
        ResponseEntity<String> response = restTemplate.getForEntity("/v1/status", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getStatusIncludesWaterLevelAndWarningFields() {
        Telemetry telemetry = new Telemetry();
        telemetry.setTime(OffsetDateTime.now());
        telemetry.setWaterLevelPct((short) 10);
        telemetryRepository.save(telemetry);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("ADMIN");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange("/v1/status", HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("water_level_pct");
        assertThat(response.getBody()).contains("water_warning");
        assertThat(response.getBody()).contains("\"water_warning\":true");
    }

    @Test
    void getStatusWaterWarningFalseWhenLevelAbove20() {
        Telemetry telemetry = new Telemetry();
        telemetry.setTime(OffsetDateTime.now());
        telemetry.setWaterLevelPct((short) 50);
        telemetryRepository.save(telemetry);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("ADMIN");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange("/v1/status", HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"water_warning\":false");
    }
}
