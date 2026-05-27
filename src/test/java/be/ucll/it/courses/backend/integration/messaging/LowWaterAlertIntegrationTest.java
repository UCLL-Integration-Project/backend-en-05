package be.ucll.it.courses.backend.integration.messaging;

import be.ucll.it.courses.backend.controller.dto.EventRequest;
import be.ucll.it.courses.backend.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class LowWaterAlertIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private be.ucll.it.courses.backend.repository.DeviceRepository deviceRepository;

    @Test
    void testLowWaterAlertFlow() throws Exception {
        deviceRepository.deleteById("ESP32-01");
        deviceRepository.save(new be.ucll.it.courses.backend.model.Device("ESP32-01", "token-01", "Test Robot"));

        // 1. Connect to WebSocket
        WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(
                List.of(new WebSocketTransport(new StandardWebSocketClient()))));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        StompSession session = stompClient.connectAsync(String.format("ws://localhost:%d/ws", port),
                new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);

        BlockingQueue<Map<String, Object>> alertsQueue = new LinkedBlockingDeque<>();

        session.subscribe("/topic/alerts", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                alertsQueue.offer((Map<String, Object>) payload);
            }
        });

        // 2. Trigger low water warning
        EventRequest warningRequest = new EventRequest(
                OffsetDateTime.now(),
                25.0f,
                (short) 80,
                (short) 18,
                null,
                null,
                null,
                null,
                null,
                "low_water"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer token-01");
        headers.set("X-Device-ID", "ESP32-01");
        HttpEntity<EventRequest> warningEntity = new HttpEntity<>(warningRequest, headers);

        ResponseEntity<Void> warningResponse = restTemplate.postForEntity("/v1/events", warningEntity, Void.class);
        assertThat(warningResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 3. Verify WebSocket warning message
        Map<String, Object> warningAlert = alertsQueue.poll(5, TimeUnit.SECONDS);
        assertThat(warningAlert).isNotNull();
        assertThat(warningAlert.get("type")).isEqualTo("low_water_warning");
        assertThat(((Number) warningAlert.get("water_level_pct")).shortValue()).isEqualTo((short) 18);

        // 4. Trigger low water recovery
        EventRequest recoveryRequest = new EventRequest(
                OffsetDateTime.now().plusSeconds(6),
                25.0f,
                (short) 80,
                (short) 30,
                null,
                null,
                null,
                null,
                null,
                "fire"
        );
        HttpEntity<EventRequest> recoveryEntity = new HttpEntity<>(recoveryRequest, headers);

        ResponseEntity<Void> recoveryResponse = restTemplate.postForEntity("/v1/events", recoveryEntity, Void.class);
        assertThat(recoveryResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 5. Verify WebSocket recovery message
        Map<String, Object> recoveryAlert = alertsQueue.poll(5, TimeUnit.SECONDS);
        assertThat(recoveryAlert).isNotNull();
        assertThat(recoveryAlert.get("type")).isEqualTo("low_water_cleared");
    }
}
