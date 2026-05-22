package be.ucll.it.courses.backend.listener;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import be.ucll.it.courses.backend.service.RobotStatusService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketEventListener {

    private final RobotStatusService robotStatusService;
    private final Map<String, String> sessionToDevice = new ConcurrentHashMap<>();

    public WebSocketEventListener(RobotStatusService robotStatusService) {
        this.robotStatusService = robotStatusService;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String deviceId = headerAccessor.getFirstNativeHeader("X-Device-ID");

        if (deviceId != null) {
            String sessionId = headerAccessor.getSessionId();
            sessionToDevice.put(sessionId, deviceId);
            
            // Record Heartbeat (This will trigger the "robot_online" broadcast)
            robotStatusService.recordHeartbeat(deviceId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String deviceId = sessionToDevice.remove(sessionId);

        if (deviceId != null) {
            // Mark Offline (This will trigger the "robot_offline" broadcast)
            robotStatusService.markOffline(deviceId);
        }
    }
}
