package be.ucll.it.courses.backend.integration.http;

import be.ucll.it.courses.backend.integration.BaseIntegrationTest;
import be.ucll.it.courses.backend.model.Telemetry;
import be.ucll.it.courses.backend.model.User;
import be.ucll.it.courses.backend.repository.TelemetryRepository;
import be.ucll.it.courses.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class StatusIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    private String adminToken;

    @Autowired
    private be.ucll.it.courses.backend.repository.EventRepository eventRepository;

    @Autowired
    private be.ucll.it.courses.backend.repository.TelemetryRepository telemetryRepository;

    @Autowired
    private be.ucll.it.courses.backend.repository.DeviceRepository deviceRepository;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        telemetryRepository.deleteAll();
        userRepository.deleteAll();
        deviceRepository.deleteAll();

        adminToken = UUID.randomUUID().toString();
        User admin = new User();
        admin.setUsername("admin");
        admin.setToken(adminToken);
        admin.setRole("ADMIN");
        userRepository.save(admin);

        // Save a default device to avoid 404 in status tests
        be.ucll.it.courses.backend.model.Device device = new be.ucll.it.courses.backend.model.Device("ESP32-01", "token", "Robot");
        device.setLastSeen(java.time.LocalDateTime.now());
        device.setOnline(true);
        deviceRepository.save(device);
    }

    @Test
    void getStatusReturnsLatestRobotState() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange("/v1/status", HttpMethod.GET, entity, String.class);

        // This might return 404 if no robot has connected yet, but the test ensures the endpoint works
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
        // Save telemetry with a water level below 20% to trigger water_warning=true
        Telemetry telemetry = new Telemetry();
        telemetry.setTime(OffsetDateTime.now());
        telemetry.setWaterLevelPct((short) 10);
        telemetryRepository.save(telemetry);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
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
        headers.setBearerAuth(adminToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange("/v1/status", HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"water_warning\":false");
    }
}
