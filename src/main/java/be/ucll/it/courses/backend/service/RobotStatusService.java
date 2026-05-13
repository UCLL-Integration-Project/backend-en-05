package be.ucll.it.courses.backend.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class RobotStatusService {

    private static final long OFFLINE_THRESHOLD_MS = 30_000;

    private volatile Instant lastHeartbeat = null;
    private volatile boolean online = false;

    private final SimpMessagingTemplate messagingTemplate;

    public RobotStatusService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void recordHeartbeat(String deviceId) {
        lastHeartbeat = Instant.now();
        if (!online) {
            online = true;
            broadcastStatus(deviceId, true);
        }
    }

    public boolean isOnline() {
        return online;
    }

    @Scheduled(fixedDelay = 5000)
    public void checkHeartbeat() {
        if (online && lastHeartbeat != null) {
            long silenceMs = Instant.now().toEpochMilli() - lastHeartbeat.toEpochMilli();
            if (silenceMs > OFFLINE_THRESHOLD_MS) {
                online = false;
                broadcastStatus("unknown", false);
            }
        }
    }

    private void broadcastStatus(String deviceId, boolean isOnline) {
        messagingTemplate.convertAndSend("/topic/status", Map.of(
            "deviceId", deviceId,
            "online", isOnline,
            "timestamp", Instant.now().toString()
        ));
    }
}
