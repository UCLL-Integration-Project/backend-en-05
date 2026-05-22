package be.ucll.it.courses.backend.unit.service;

import be.ucll.it.courses.backend.controller.dto.RobotStatusResponse;
import be.ucll.it.courses.backend.model.Event;
import be.ucll.it.courses.backend.model.RobotMode;
import be.ucll.it.courses.backend.model.Telemetry;
import be.ucll.it.courses.backend.repository.EventRepository;
import be.ucll.it.courses.backend.repository.TelemetryRepository;
import be.ucll.it.courses.backend.service.StatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class StatusServiceTest {

    private StatusService statusService;

    @Mock
    private TelemetryRepository telemetryRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private be.ucll.it.courses.backend.repository.DeviceRepository deviceRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        statusService = new StatusService(telemetryRepository, eventRepository, deviceRepository);
    }

    @Test
    void getRobotStatusReturnsEmptyWhenNoDevices() {
        when(deviceRepository.findAll()).thenReturn(java.util.List.of());

        Optional<RobotStatusResponse> status = statusService.getRobotStatus();

        assertThat(status).isEmpty();
    }

    @Test
    void getRobotStatusReturnsOfflineWhenDeviceIsOffline() {
        be.ucll.it.courses.backend.model.Device device = new be.ucll.it.courses.backend.model.Device("ESP32-01", "token", "Robot");
        device.setOnline(false);
        device.setLastSeen(java.time.LocalDateTime.now());
        
        when(deviceRepository.findAll()).thenReturn(java.util.List.of(device));
        when(telemetryRepository.findFirstByOrderByTimeDesc()).thenReturn(Optional.empty());
        when(eventRepository.findFirstByOrderByTimestampDesc()).thenReturn(Optional.empty());

        Optional<RobotStatusResponse> status = statusService.getRobotStatus();

        assertThat(status).isPresent();
        assertThat(status.get().mode()).isEqualTo(RobotMode.offline);
        assertThat(status.get().wifi_connected()).isFalse();
    }

    @Test
    void getRobotStatusReturnsPatrollingWhenOnlineAndNoFire() {
        be.ucll.it.courses.backend.model.Device device = new be.ucll.it.courses.backend.model.Device("ESP32-01", "token", "Robot");
        device.setOnline(true);
        device.setLastSeen(java.time.LocalDateTime.now());

        when(deviceRepository.findAll()).thenReturn(java.util.List.of(device));
        
        Telemetry recentTelemetry = new Telemetry();
        recentTelemetry.setTime(OffsetDateTime.now());
        recentTelemetry.setPumpActive(false);
        
        when(telemetryRepository.findFirstByOrderByTimeDesc()).thenReturn(Optional.of(recentTelemetry));
        
        Event extinguishedEvent = new Event();
        extinguishedEvent.setIsExtinguished(true);
        when(eventRepository.findFirstByOrderByTimestampDesc()).thenReturn(Optional.of(extinguishedEvent));

        Optional<RobotStatusResponse> status = statusService.getRobotStatus();

        assertThat(status).isPresent();
        assertThat(status.get().mode()).isEqualTo(RobotMode.patrolling);
        assertThat(status.get().wifi_connected()).isTrue();
    }
}
