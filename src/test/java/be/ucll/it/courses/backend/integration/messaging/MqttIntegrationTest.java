package be.ucll.it.courses.backend.integration.messaging;

import be.ucll.it.courses.backend.integration.BaseIntegrationTest;
import be.ucll.it.courses.backend.model.User;
import be.ucll.it.courses.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;

public class MqttIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    @Qualifier("handler")
    private MessageHandler handler;

    private String userToken;

    @Autowired
    private be.ucll.it.courses.backend.repository.EventRepository eventRepository;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        userRepository.deleteAll();
        userToken = UUID.randomUUID().toString();
        User user = new User();
        user.setUsername("testuser");
        user.setToken(userToken);
        userRepository.save(user);
    }

    @Test
    void mqttMessageReflectedInStatus() throws Exception {
        // Direct call to the handler to simulate receiving a message
        // The message should result in a telemetry record being saved
        String payload = "{\"mode\":\"fire_detected\",\"battery_pct\":75,\"temperature\":25.5}";
        handler.handleMessage(new GenericMessage<>(payload));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(userToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // StatusService returns 404 if no telemetry exists. 
        // We wait for the async handler to save it.
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            ResponseEntity<String> response = restTemplate.exchange("/v1/status", HttpMethod.GET, entity, String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("patrolling"); // Default mode if wifi is online
        });
    }
}
