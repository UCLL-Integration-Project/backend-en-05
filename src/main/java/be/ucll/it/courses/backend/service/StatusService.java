package be.ucll.it.courses.backend.service;

import be.ucll.it.courses.backend.controller.dto.RobotStatusResponse;
import be.ucll.it.courses.backend.model.Event;
import be.ucll.it.courses.backend.model.RobotMode;
import be.ucll.it.courses.backend.model.Telemetry;
import be.ucll.it.courses.backend.repository.EventRepository;
import be.ucll.it.courses.backend.repository.TelemetryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class StatusService {

    private final TelemetryRepository telemetryRepository;
    private final EventRepository eventRepository;

    @Autowired
    public StatusService(TelemetryRepository telemetryRepository, EventRepository eventRepository) {
        this.telemetryRepository = telemetryRepository;
        this.eventRepository = eventRepository;
    }

    public Optional<RobotStatusResponse> getRobotStatus() {
        Optional<Telemetry> latestTelemetry = telemetryRepository.findFirstByOrderByTimeDesc();
        
        if (latestTelemetry.isEmpty()) {
            return Optional.empty();
        }

        Telemetry telemetry = latestTelemetry.get();
        Optional<Event> latestEvent = eventRepository.findFirstByOrderByTimestampDesc();

        OffsetDateTime lastSeen = telemetry.getTime();
        boolean wifiConnected = lastSeen.isAfter(OffsetDateTime.now().minusMinutes(1));
        
        RobotMode mode;
        if (!wifiConnected) {
            mode = RobotMode.offline;
        } else if (latestEvent.isPresent() && !latestEvent.get().getIsExtinguished()) {
            // If the latest event is not extinguished, we are in fire_detected or extinguishing mode.
            // Since we don't have a specific field for pump status in Event, 
            // but Telemetry has pumpActive.
            if (Boolean.TRUE.equals(telemetry.getPumpActive())) {
                mode = RobotMode.extinguishing;
            } else {
                mode = RobotMode.fire_detected;
            }
        } else {
            mode = RobotMode.patrolling;
        }

        Integer batteryPct = 100;
        if (latestEvent.isPresent() && latestEvent.get().getBatteryPct() != null) {
            batteryPct = latestEvent.get().getBatteryPct();
        }
        // Alternatively, use telemetry voltage if we had a mapping.

        return Optional.of(new RobotStatusResponse(
            mode,
            batteryPct,
            wifiConnected,
            lastSeen,
            latestEvent.map(Event::getIncidentId).orElse(null)
        ));
    }
}
