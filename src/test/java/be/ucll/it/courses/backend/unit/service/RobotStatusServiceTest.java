package be.ucll.it.courses.backend.unit.service;

import be.ucll.it.courses.backend.model.Device;
import be.ucll.it.courses.backend.repository.DeviceRepository;
import be.ucll.it.courses.backend.repository.TelemetryRepository;
import be.ucll.it.courses.backend.service.RobotStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RobotStatusServiceTest {

    private RobotStatusService service;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private TelemetryRepository telemetryRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private be.ucll.it.courses.backend.service.TelemetryCacheService telemetryCacheService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new RobotStatusService(deviceRepository, telemetryRepository, messagingTemplate, telemetryCacheService);
        // Test config matches: app.status.offline-threshold-seconds=1
        ReflectionTestUtils.setField(service, "offlineThresholdSeconds", 1L);
    }

    @Test
    void recordHeartbeatOnPreviouslyOfflineDeviceBroadcastsOnline() {
        Device device = new Device("ESP32-01", "token", "Robot");
        device.setOnline(false);
        when(deviceRepository.findById("ESP32-01")).thenReturn(Optional.of(device));

        service.recordHeartbeat("ESP32-01");

        assertThat(device.isOnline()).isTrue();
        assertThat(device.getLastSeen()).isNotNull();
        verify(deviceRepository).save(device);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/status"), payloadCaptor.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
        assertThat(payload.get("type")).isEqualTo("robot_online");
        assertThat(payload.get("device_id")).isEqualTo("ESP32-01");
    }

    @Test
    void recordHeartbeatOnAlreadyOnlineDeviceDoesNotBroadcast() {
        Device device = new Device("ESP32-01", "token", "Robot");
        device.setOnline(true);
        device.setLastSeen(LocalDateTime.now().minusSeconds(5));
        when(deviceRepository.findById("ESP32-01")).thenReturn(Optional.of(device));

        service.recordHeartbeat("ESP32-01");

        assertThat(device.isOnline()).isTrue();
        verify(deviceRepository).save(device);
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void recordHeartbeatOnUnknownDeviceDoesNothing() {
        when(deviceRepository.findById("UNKNOWN")).thenReturn(Optional.empty());

        service.recordHeartbeat("UNKNOWN");

        verify(deviceRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void markOfflineOnOnlineDeviceBroadcastsOffline() {
        Device device = new Device("ESP32-01", "token", "Robot");
        device.setOnline(true);
        device.setLastSeen(LocalDateTime.now());
        when(deviceRepository.findById("ESP32-01")).thenReturn(Optional.of(device));

        service.markOffline("ESP32-01");

        assertThat(device.isOnline()).isFalse();
        verify(deviceRepository).save(device);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/status"), payloadCaptor.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
        assertThat(payload.get("type")).isEqualTo("robot_offline");
        assertThat(payload.get("device_id")).isEqualTo("ESP32-01");
    }

    @Test
    void markOfflineOnAlreadyOfflineDeviceDoesNothing() {
        Device device = new Device("ESP32-01", "token", "Robot");
        device.setOnline(false);
        when(deviceRepository.findById("ESP32-01")).thenReturn(Optional.of(device));

        service.markOffline("ESP32-01");

        verify(deviceRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void checkHeartbeatsMarksSilentOnlineDeviceOfflineAndBroadcasts() {
        Device staleDevice = new Device("ESP32-01", "token", "Robot");
        staleDevice.setOnline(true);
        staleDevice.setLastSeen(LocalDateTime.now().minusSeconds(5));
        when(deviceRepository.findAll()).thenReturn(List.of(staleDevice));

        service.checkHeartbeats();

        assertThat(staleDevice.isOnline()).isFalse();
        verify(deviceRepository).save(staleDevice);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/status"), payloadCaptor.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
        assertThat(payload.get("type")).isEqualTo("robot_offline");
        assertThat(payload.get("device_id")).isEqualTo("ESP32-01");
    }

    @Test
    void checkHeartbeatsLeavesRecentlySeenDeviceAlone() {
        Device freshDevice = new Device("ESP32-01", "token", "Robot");
        freshDevice.setOnline(true);
        freshDevice.setLastSeen(LocalDateTime.now());
        when(deviceRepository.findAll()).thenReturn(List.of(freshDevice));

        service.checkHeartbeats();

        assertThat(freshDevice.isOnline()).isTrue();
        verify(deviceRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void checkHeartbeatsIgnoresAlreadyOfflineDevices() {
        Device offlineDevice = new Device("ESP32-01", "token", "Robot");
        offlineDevice.setOnline(false);
        offlineDevice.setLastSeen(LocalDateTime.now().minusHours(1));
        when(deviceRepository.findAll()).thenReturn(List.of(offlineDevice));

        service.checkHeartbeats();

        assertThat(offlineDevice.isOnline()).isFalse();
        verify(deviceRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }
}
