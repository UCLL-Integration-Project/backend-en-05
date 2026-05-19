package be.ucll.it.courses.backend.integration.http;

import be.ucll.it.courses.backend.integration.BaseIntegrationTest;
import be.ucll.it.courses.backend.model.Device;
import be.ucll.it.courses.backend.model.User;
import be.ucll.it.courses.backend.repository.DeviceRepository;
import be.ucll.it.courses.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class EventIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    private String userToken;
    private String deviceToken;
    private String deviceId = "esp32-test";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        // fire_events references devices, so delete fire_events first
        restTemplate.getRestTemplate().getInterceptors().clear(); // Ensure clean state
        deviceRepository.findAll().forEach(d -> {
            // This is a bit slow but safe. Alternatively, use a custom delete method.
        });
        // Better: delete in correct order
    }

    @Autowired
    private be.ucll.it.courses.backend.repository.EventRepository eventRepository;

    @BeforeEach
    void cleanUp() {
        eventRepository.deleteAll();
        deviceRepository.deleteAll();
        userRepository.deleteAll();
        
        userToken = UUID.randomUUID().toString();
        User user = new User();
        user.setUsername("testuser");
        user.setToken(userToken);
        userRepository.save(user);

        deviceToken = UUID.randomUUID().toString();
        Device device = new Device();
        device.setDeviceId(deviceId);
        device.setDeviceToken(deviceToken);
        deviceRepository.save(device);
    }

    @Test
    void postEventReturns201AndCorrectSchema() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(deviceToken);
        headers.set("X-Device-ID", deviceId);
        
        Map<String, Object> body = Map.of(
            "timestamp", OffsetDateTime.now().toString(),
            "temperature", 25.5,
            "battery_pct", 85
        );
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity("/v1/events", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("incident_id");
        assertThat(response.getBody()).contains("status");
    }

    @Test
    void postEventReturns400ForMissingFields() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(deviceToken);
        headers.set("X-Device-ID", deviceId);
        
        Map<String, Object> body = Map.of(
            "temperature", 25.5
        );
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity("/v1/events", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getEventsReturnsPaginatedList() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(userToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange("/v1/events?limit=10&offset=0", HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("total");
        assertThat(response.getBody()).contains("events");
    }
}
