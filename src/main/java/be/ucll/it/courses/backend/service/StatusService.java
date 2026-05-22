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
    private final be.ucll.it.courses.backend.repository.DeviceRepository deviceRepository;

    @Autowired
    public StatusService(TelemetryRepository telemetryRepository, 
                         EventRepository eventRepository,
                         be.ucll.it.courses.backend.repository.DeviceRepository deviceRepository) {
        this.telemetryRepository = telemetryRepository;
        this.eventRepository = eventRepository;
        this.deviceRepository = deviceRepository;
    }

    public Optional<RobotStatusResponse> getRobotStatus() {
        // Find the device that was last seen most recently
        Optional<be.ucll.it.courses.backend.model.Device> latestDevice = deviceRepository.findAll().stream()
                .filter(d -> d.getLastSeen() != null)
                .max(java.util.Comparator.comparing(be.ucll.it.courses.backend.model.Device::getLastSeen));

        if (latestDevice.isEmpty()) {
            return Optional.empty();
        }

        be.ucll.it.courses.backend.model.Device device = latestDevice.get();
        Optional<Telemetry> latestTelemetry = telemetryRepository.findFirstByOrderByTimeDesc();
        Optional<Event> latestEvent = eventRepository.findFirstByOrderByTimestampDesc();

        java.time.OffsetDateTime lastSeen = device.getLastSeen().atOffset(java.time.ZoneOffset.UTC);
        boolean wifiConnected = device.isOnline();
        
        RobotMode mode;
        if (!wifiConnected) {
            mode = RobotMode.offline;
        } else if (latestEvent.isPresent() && !Boolean.TRUE.equals(latestEvent.get().getIsExtinguished())) {
            if (latestTelemetry.isPresent() && Boolean.TRUE.equals(latestTelemetry.get().getPumpActive())) {
                mode = RobotMode.extinguishing;
            } else {
                mode = RobotMode.fire_detected;
            }
        } else {
            mode = RobotMode.patrolling;
        }

        Short batteryPct = 100;
        if (latestEvent.isPresent() && latestEvent.get().getBatteryPct() != null) {
            batteryPct = latestEvent.get().getBatteryPct();
        }

        Short waterLevelPct = latestTelemetry.map(Telemetry::getWaterLevelPct).orElse(null);
        boolean waterWarning = waterLevelPct != null && waterLevelPct < 20;

        return Optional.of(new RobotStatusResponse(
            mode,
            batteryPct,
            wifiConnected,
            lastSeen,
            latestEvent.map(Event::getIncidentId).orElse(null),
            waterLevelPct,
            waterWarning
        ));
    }
}
