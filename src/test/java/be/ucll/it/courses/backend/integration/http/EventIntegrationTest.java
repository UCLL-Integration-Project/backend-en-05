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

    @Test
    void postEventWithValidEventTypeReturns201() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(deviceToken);
        headers.set("X-Device-ID", deviceId);

        Map<String, Object> body = Map.of(
            "timestamp", OffsetDateTime.now().toString(),
            "temperature", 30.0,
            "battery_pct", 70,
            "event_type", "low_water"
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity("/v1/events", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("incident_id");
    }

    @Test
    void postEventWithInvalidEventTypeReturns400() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(deviceToken);
        headers.set("X-Device-ID", deviceId);

        Map<String, Object> body = Map.of(
            "timestamp", OffsetDateTime.now().toString(),
            "temperature", 30.0,
            "battery_pct", 70,
            "event_type", "invalid_type"
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity("/v1/events", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getEventsFilteredByEventTypeReturnsOnlyMatchingEvents() {
        // Create a "fire" event via POST
        HttpHeaders deviceHeaders = new HttpHeaders();
        deviceHeaders.setBearerAuth(deviceToken);
        deviceHeaders.set("X-Device-ID", deviceId);

        Map<String, Object> fireBody = Map.of(
            "timestamp", OffsetDateTime.now().toString(),
            "temperature", 80.0,
            "battery_pct", 50,
            "event_type", "fire"
        );
        restTemplate.postForEntity("/v1/events", new HttpEntity<>(fireBody, deviceHeaders), String.class);

        Map<String, Object> waterBody = Map.of(
            "timestamp", OffsetDateTime.now().toString(),
            "temperature", 20.0,
            "battery_pct", 60,
            "event_type", "low_water"
        );
        restTemplate.postForEntity("/v1/events", new HttpEntity<>(waterBody, deviceHeaders), String.class);

        // GET filtered by event_type=fire
        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(userToken);
        HttpEntity<Void> getEntity = new HttpEntity<>(userHeaders);

        ResponseEntity<String> response = restTemplate.exchange(
            "/v1/events?event_type=fire", HttpMethod.GET, getEntity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"event_type\":\"fire\"");
        // Ensure low_water event is not returned
        assertThat(response.getBody()).doesNotContain("\"event_type\":\"low_water\"");
    }

    @Test
    void getEventsResponseIncludesEventTypeField() {
        HttpHeaders deviceHeaders = new HttpHeaders();
        deviceHeaders.setBearerAuth(deviceToken);
        deviceHeaders.set("X-Device-ID", deviceId);

        Map<String, Object> body = Map.of(
            "timestamp", OffsetDateTime.now().toString(),
            "temperature", 25.0,
            "battery_pct", 90,
            "event_type", "fire"
        );
        restTemplate.postForEntity("/v1/events", new HttpEntity<>(body, deviceHeaders), String.class);

        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(userToken);
        HttpEntity<Void> getEntity = new HttpEntity<>(userHeaders);

        ResponseEntity<String> response = restTemplate.exchange("/v1/events", HttpMethod.GET, getEntity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("event_type");
    }
}
