package be.ucll.it.courses.backend.integration.messaging;

import be.ucll.it.courses.backend.integration.BaseIntegrationTest;
import be.ucll.it.courses.backend.model.Device;
import be.ucll.it.courses.backend.repository.DeviceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class LocationBroadcastIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DeviceRepository deviceRepository;

    @Test
    void broadcastsLocationUpdateWithinOneSecondOfTelemetryPost() throws Exception {
<<<<<<< HEAD
=======
        deviceRepository.deleteById("ESP32-01");
>>>>>>> bc95b2114f98259bc52f43f19dc8cedbf0b26135
        deviceRepository.save(new Device("ESP32-01", "token-01", "Test Robot"));

        WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(
                List.of(new WebSocketTransport(new StandardWebSocketClient()))));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        StompSession session = stompClient.connectAsync(
                String.format("ws://localhost:%d/ws", port),
                new StompSessionHandlerAdapter() {}
        ).get(5, TimeUnit.SECONDS);

        BlockingQueue<Map<String, Object>> locationQueue = new LinkedBlockingDeque<>();

        session.subscribe("/topic/location", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                locationQueue.offer((Map<String, Object>) payload);
            }
        });

        Map<String, Object> telemetryPayload = Map.of(
                "latitude", 50.8798,
                "longitude", 4.7005,
                "accuracy_m", 45.0
        );

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("Authorization", "Bearer token-01");
        httpHeaders.set("X-Device-ID", "ESP32-01");
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/v1/telemetry",
                new HttpEntity<>(telemetryPayload, httpHeaders),
                Void.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Map<String, Object> message = locationQueue.poll(1, TimeUnit.SECONDS);
        assertThat(message).isNotNull();
        assertThat(message.get("type")).isEqualTo("location_update");
        assertThat(((Number) message.get("latitude")).doubleValue()).isEqualTo(50.8798);
        assertThat(((Number) message.get("longitude")).doubleValue()).isEqualTo(4.7005);
        assertThat(((Number) message.get("accuracy_m")).doubleValue()).isEqualTo(45.0);
        assertThat(message.get("timestamp")).isNotNull();
    }
}
