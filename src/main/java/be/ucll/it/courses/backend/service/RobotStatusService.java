package be.ucll.it.courses.backend.service;

import be.ucll.it.courses.backend.model.Device;
import be.ucll.it.courses.backend.repository.DeviceRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RobotStatusService {

    @org.springframework.beans.factory.annotation.Value("${app.status.offline-threshold-seconds:30}")
    private long offlineThresholdSeconds;

    private final DeviceRepository deviceRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public RobotStatusService(DeviceRepository deviceRepository, SimpMessagingTemplate messagingTemplate) {
        this.deviceRepository = deviceRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public void recordHeartbeat(String deviceId) {
        Optional<Device> deviceOpt = deviceRepository.findById(deviceId);
        if (deviceOpt.isPresent()) {
            Device device = deviceOpt.get();
            boolean wasOffline = !device.isOnline();
            device.setOnline(true);
            device.setLastSeen(LocalDateTime.now());
            deviceRepository.save(device);

            if (wasOffline) {
                broadcastOnlineStatus(deviceId);
            }
        }
    }

    @Transactional
    public void markOffline(String deviceId) {
        Optional<Device> deviceOpt = deviceRepository.findById(deviceId);
        if (deviceOpt.isPresent()) {
            Device device = deviceOpt.get();
            if (device.isOnline()) {
                device.setOnline(false);
                deviceRepository.save(device);
                broadcastOfflineStatus(device);
            }
        }
    }

    public Map<String, Object> getStatus() {
        Optional<Device> latestDevice = deviceRepository.findAll().stream()
                .filter(d -> d.getLastSeen() != null)
                .max(java.util.Comparator.comparing(Device::getLastSeen));

        if (latestDevice.isEmpty()) {
            return Map.of("online", false, "deviceId", "unknown");
        }

        Device device = latestDevice.get();
        return Map.of(
            "online", device.isOnline(),
            "deviceId", device.getDeviceId(),
            "wifi_connected", device.isOnline(),
            "mode", device.isOnline() ? "patrolling" : "offline",
            "battery_pct", 100,
            "last_seen", device.getLastSeen().toString(),
            "last_event_id", ""
        );
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void checkHeartbeats() {
        List<Device> devices = deviceRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Device device : devices) {
            if (device.isOnline() && device.getLastSeen() != null) {
                long silenceSeconds = now.toEpochSecond(ZoneOffset.UTC) - device.getLastSeen().toEpochSecond(ZoneOffset.UTC);
                if (silenceSeconds > offlineThresholdSeconds) {
                    device.setOnline(false);
                    deviceRepository.save(device);
                    broadcastOfflineStatus(device);
                }
            }
        }
    }

    private void broadcastOnlineStatus(String deviceId) {
        messagingTemplate.convertAndSend("/topic/status", Map.of(
            "type", "robot_online",
            "device_id", deviceId,
            "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        ));
    }

    private void broadcastOfflineStatus(Device device) {
        messagingTemplate.convertAndSend("/topic/status", Map.of(
            "type", "robot_offline",
            "device_id", device.getDeviceId(),
            "last_seen", device.getLastSeen() != null ? 
                device.getLastSeen().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "unknown"
        ));
    }
}
