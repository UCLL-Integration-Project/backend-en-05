package be.ucll.it.courses.backend.controller;

import be.ucll.it.courses.backend.controller.dto.RobotTelemetryMessage;
import be.ucll.it.courses.backend.service.RobotStatusService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class RobotWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final RobotStatusService robotStatusService;

    public RobotWebSocketController(SimpMessagingTemplate messagingTemplate, RobotStatusService robotStatusService) {
        this.messagingTemplate = messagingTemplate;
        this.robotStatusService = robotStatusService;
    }

    /**
     * Handles telemetry and fire sensor data sent by the robot.
     * Robot sends to: /app/telemetry
     */
    @MessageMapping("/telemetry")
    public void handleTelemetry(@Payload RobotTelemetryMessage telemetry) {
        // Record heartbeat
        robotStatusService.recordHeartbeat(telemetry.getDeviceId());

        // Broadcast telemetry to dashboards subscribed to /topic/events
        messagingTemplate.convertAndSend("/topic/events", telemetry);

        if (telemetry.isFireDetected()) {
            // Optional: Broadcast specialized alert if needed
            System.out.println("Fire alert from: " + telemetry.getDeviceId());
        }
    }

    /**
     * Handles periodic pings from the robot.
     * Robot sends to: /app/ping
     */
    @MessageMapping("/ping")
    public void handlePing(@Payload Map<String, String> payload) {
        String deviceId = payload.get("device_id");
        if (deviceId != null) {
            robotStatusService.recordHeartbeat(deviceId);
        }
    }
}
