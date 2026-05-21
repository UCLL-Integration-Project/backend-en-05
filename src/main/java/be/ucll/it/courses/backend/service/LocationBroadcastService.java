package be.ucll.it.courses.backend.service;

import be.ucll.it.courses.backend.controller.dto.LocationUpdateMessage;
import be.ucll.it.courses.backend.model.Telemetry;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class LocationBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    public LocationBroadcastService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastIfLocationPresent(Telemetry telemetry) {
        if (telemetry.getLatitude() == null || telemetry.getLongitude() == null) {
            return;
        }
        var message = new LocationUpdateMessage(
                "location_update",
                telemetry.getLatitude(),
                telemetry.getLongitude(),
                telemetry.getAccuracyM() != null ? telemetry.getAccuracyM() : 0.0,
                Instant.now().toString()
        );
        messagingTemplate.convertAndSend("/topic/location", message);
    }
}
