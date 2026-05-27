package be.ucll.it.courses.backend.integration.messaging;

import be.ucll.it.courses.backend.integration.BaseIntegrationTest;
import be.ucll.it.courses.backend.model.Device;
import be.ucll.it.courses.backend.repository.DeviceRepository;
import be.ucll.it.courses.backend.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.support.AbstractSubscribableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
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

    @Autowired
    private ApplicationContext applicationContext;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        deviceRepository.deleteAll();
    }

    /** Records a (channelBeanName, payload) pair for each message seen. */
    private record CapturedMessage(String channel, String destination, Object payload) {}

    @Test
    void testRobotOnlineOfflineCycle() throws Exception {
        String deviceId = "ESP32-01";
        deviceRepository.save(new Device(deviceId, "token-01", "Test Robot"));

        // Attach a capturing interceptor to EVERY subscribable channel in the
        // context. Spring's WebSocket infrastructure has at least
        // clientInboundChannel, brokerChannel and clientOutboundChannel; we
        // don't assume which one our broadcasts traverse — we record on all.
        List<Map.Entry<String, AbstractSubscribableChannel>> channels = new ArrayList<>();
        for (String name : applicationContext.getBeanNamesForType(AbstractSubscribableChannel.class)) {
            AbstractSubscribableChannel ch = applicationContext.getBean(name, AbstractSubscribableChannel.class);
            channels.add(Map.entry(name, ch));
        }

        List<CapturedMessage> captured = new CopyOnWriteArrayList<>();
        List<Map.Entry<AbstractSubscribableChannel, ChannelInterceptor>> attached = new ArrayList<>();
        for (Map.Entry<String, AbstractSubscribableChannel> entry : channels) {
            String channelName = entry.getKey();
            AbstractSubscribableChannel ch = entry.getValue();
            ChannelInterceptor interceptor = new ChannelInterceptor() {
                @Override
                public Message<?> preSend(Message<?> message, MessageChannel channel) {
                    SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
                    captured.add(new CapturedMessage(channelName, accessor.getDestination(), message.getPayload()));
                    return message;
                }
            };
            ch.addInterceptor(interceptor);
            attached.add(Map.entry(ch, interceptor));
        }

        try {
            WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(
                    List.of(new WebSocketTransport(new StandardWebSocketClient()))));
            stompClient.setMessageConverter(new MappingJackson2MessageConverter());

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

            // 1. Send ping to go online.
            StompHeaders pingHeaders = new StompHeaders();
            pingHeaders.setDestination("/app/ping");
            session.send(pingHeaders, Map.of("device_id", deviceId));

            Map<String, Object> onlineMsg = statusQueue.poll(5, TimeUnit.SECONDS);
            assertThat(onlineMsg).isNotNull();
            assertThat(onlineMsg.get("type")).isEqualTo("robot_online");

            long start = System.currentTimeMillis();
            boolean online = false;
            while (System.currentTimeMillis() - start < 2000) {
                Device d = deviceRepository.findById(deviceId).orElseThrow();
                if (d.isOnline()) { online = true; break; }
                Thread.sleep(100);
            }
            assertThat(online).isTrue();

            // 2. Wait for the @Scheduled checkHeartbeats to detect silence and
            // mark the device offline. Verify both DB state and that some
            // channel saw a robot_offline broadcast.
            long deadline = System.currentTimeMillis() + 15000;
            boolean offline = false;
            boolean offlineBroadcastSeen = false;
            while (System.currentTimeMillis() < deadline && (!offline || !offlineBroadcastSeen)) {
                if (!offline) {
                    Device d = deviceRepository.findById(deviceId).orElseThrow();
                    if (!d.isOnline()) offline = true;
                }
                if (!offlineBroadcastSeen) {
                    offlineBroadcastSeen = captured.stream().anyMatch(cm ->
                            cm.payload() instanceof Map
                                    && "robot_offline".equals(((Map<?, ?>) cm.payload()).get("type"))
                                    && deviceId.equals(((Map<?, ?>) cm.payload()).get("device_id")));
                }
                if (!offline || !offlineBroadcastSeen) {
                    Thread.sleep(100);
                }
            }

            assertThat(offline)
                    .as("Device should be marked offline in DB by scheduler within 15s. "
                            + "All captured channel traffic: %s", captured)
                    .isTrue();
            assertThat(offlineBroadcastSeen)
                    .as("Expected a robot_offline broadcast for %s. "
                            + "Channel beans found: %s. "
                            + "Captured %d messages across all channels: %s",
                            deviceId,
                            channels.stream().map(Map.Entry::getKey).toList(),
                            captured.size(),
                            captured)
                    .isTrue();
        } finally {
            for (Map.Entry<AbstractSubscribableChannel, ChannelInterceptor> e : attached) {
                e.getKey().removeInterceptor(e.getValue());
            }
        }
    }
}
