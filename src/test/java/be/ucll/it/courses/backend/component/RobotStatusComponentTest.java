package be.ucll.it.courses.backend.component;

import be.ucll.it.courses.backend.model.Device;
import be.ucll.it.courses.backend.repository.DeviceRepository;
import be.ucll.it.courses.backend.service.RobotStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class RobotStatusComponentTest {

    @Autowired
    private RobotStatusService robotStatusService;

    @Autowired
    private DeviceRepository deviceRepository;

    @Test
    void testHeartbeatUpdatesOnlineStatus() {
        String deviceId = "ESP32-99";
        Device device = new Device(deviceId, "token-99", "Test Robot");
        device.setOnline(false);
        deviceRepository.save(device);

        // Record heartbeat
        robotStatusService.recordHeartbeat(deviceId);

        // Verify status
        Device updated = deviceRepository.findById(deviceId).orElseThrow();
        assertThat(updated.isOnline()).isTrue();
        assertThat(updated.getLastSeen()).isNotNull();
    }

    @Test
    void testMarkOffline() {
        String deviceId = "ESP32-99";
        Device device = new Device(deviceId, "token-99", "Test Robot");
        device.setOnline(true);
        device.setLastSeen(LocalDateTime.now());
        deviceRepository.save(device);

        // Mark offline
        robotStatusService.markOffline(deviceId);

        // Verify status
        Device updated = deviceRepository.findById(deviceId).orElseThrow();
        assertThat(updated.isOnline()).isFalse();
    }
}
