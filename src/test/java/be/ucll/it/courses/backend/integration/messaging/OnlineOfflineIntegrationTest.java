package be.ucll.it.courses.backend.integration.messaging;

import be.ucll.it.courses.backend.integration.BaseIntegrationTest;
import be.ucll.it.courses.backend.model.Device;
import be.ucll.it.courses.backend.repository.DeviceRepository;
import be.ucll.it.courses.backend.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
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

public class OnlineOfflineIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private EventRepository eventRepository;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        deviceRepository.deleteAll();
    }

    @Test
    void testRobotOnlineOfflineCycle() throws Exception {
        String deviceId = "ESP32-01";
        if (deviceRepository.findById(deviceId).isEmpty()) {
            deviceRepository.save(new Device(deviceId, "token-01", "Test Robot"));
        }

        WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(
                List.of(new WebSocketTransport(new StandardWebSocketClient()))));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        // A TaskScheduler is required for SockJS/STOMP heartbeats. Without it,
        // the client cannot respond to server heartbeats and the broker may
        // consider the session idle and silently stop dispatching messages —
        // which is what causes the robot_offline broadcast (sent ~2s after
        // ping by the scheduler) to never reach the test client in CI.
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.afterPropertiesSet();
        stompClient.setTaskScheduler(taskScheduler);
        stompClient.setDefaultHeartbeat(new long[]{10000, 10000});

        BlockingQueue<Map<String, Object>> statusQueue = new LinkedBlockingDeque<>();

        StompSession session = stompClient.connectAsync(String.format("ws://localhost:%d/ws", port),
                new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);

        session.subscribe("/topic/status", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                statusQueue.offer((Map<String, Object>) payload);
            }
        });

        // 1. Send Ping to go Online
        StompHeaders pingHeaders = new StompHeaders();
        pingHeaders.setDestination("/app/ping");
        session.send(pingHeaders, Map.of("device_id", deviceId));

        // Wait for robot_online broadcast
        Map<String, Object> onlineMsg = statusQueue.poll(5, TimeUnit.SECONDS);
        assertThat(onlineMsg).isNotNull();
        assertThat(onlineMsg.get("type")).isEqualTo("robot_online");
        assertThat(onlineMsg.get("device_id")).isEqualTo(deviceId);

        // Wait until DB reflects the online state (transaction may commit slightly after WS message)
        long start = System.currentTimeMillis();
        boolean online = false;
        while (System.currentTimeMillis() - start < 2000) {
            Device d = deviceRepository.findById(deviceId).orElseThrow();
            if (d.isOnline()) { online = true; break; }
            Thread.sleep(100);
        }
        assertThat(online).isTrue();

        // 2. Wait for the @Scheduled checkHeartbeats to detect silence
        // (threshold=1s in test config, scheduler runs every 1s) and broadcast
        // robot_offline. Poll DB and WS concurrently up to 20s.
        long deadline = System.currentTimeMillis() + 20000;
        boolean offline = false;
        Map<String, Object> offlineMsg = null;
        while (System.currentTimeMillis() < deadline && (!offline || offlineMsg == null)) {
            if (offlineMsg == null) {
                offlineMsg = statusQueue.poll(200, TimeUnit.MILLISECONDS);
            }
            if (!offline) {
                Device d = deviceRepository.findById(deviceId).orElseThrow();
                if (!d.isOnline()) offline = true;
            }
            if (offlineMsg == null && offline) {
                Thread.sleep(200);
            }
        }
        assertThat(offline).isTrue();
        assertThat(offlineMsg).isNotNull();
        assertThat(offlineMsg.get("type")).isEqualTo("robot_offline");
        assertThat(offlineMsg.get("device_id")).isEqualTo(deviceId);
    }
}
