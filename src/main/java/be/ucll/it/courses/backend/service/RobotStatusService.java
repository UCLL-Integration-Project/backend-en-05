package be.ucll.it.courses.backend.service;

import be.ucll.it.courses.backend.model.Device;
import be.ucll.it.courses.backend.repository.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RobotStatusService {

    private static final Logger log = LoggerFactory.getLogger(RobotStatusService.class);

    @org.springframework.beans.factory.annotation.Value("${app.status.offline-threshold-seconds:30}")
    private long offlineThresholdSeconds;

    private final DeviceRepository deviceRepository;
    private final be.ucll.it.courses.backend.repository.TelemetryRepository telemetryRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, LocalDateTime> lastSavedTelemetry = new java.util.concurrent.ConcurrentHashMap<>();

    public RobotStatusService(DeviceRepository deviceRepository, 
                              be.ucll.it.courses.backend.repository.TelemetryRepository telemetryRepository,
                              SimpMessagingTemplate messagingTemplate) {
        this.deviceRepository = deviceRepository;
        this.telemetryRepository = telemetryRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public void processTelemetry(be.ucll.it.courses.backend.controller.dto.RobotTelemetryMessage message) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastSaved = lastSavedTelemetry.get(message.getDeviceId());

        if (lastSaved == null || java.time.Duration.between(lastSaved, now).getSeconds() >= 10) {
            be.ucll.it.courses.backend.model.Telemetry telemetry = new be.ucll.it.courses.backend.model.Telemetry();
            
            // Parse timestamp from message or use now
            if (message.getTimestamp() != null) {
                try {
                    telemetry.setTime(java.time.OffsetDateTime.parse(message.getTimestamp()));
                } catch (Exception e) {
                    telemetry.setTime(java.time.OffsetDateTime.now());
                }
            } else {
                telemetry.setTime(java.time.OffsetDateTime.now());
            }

            telemetry.setWaterLevelPct(message.getWaterLevelPct());
            telemetry.setLatitude(message.getLatitude());
            telemetry.setLongitude(message.getLongitude());
            telemetry.setPumpActive(message.isFireDetected());
            
            // Map battery_pct to battery_voltage field (approximate or just store raw)
            telemetry.setBatteryVoltage((float) message.getBatteryPct());
            
            telemetryRepository.save(telemetry);
            lastSavedTelemetry.put(message.getDeviceId(), now);
            System.out.println("Telemetry saved for device: " + message.getDeviceId());
        }
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

    @Scheduled(fixedRate = 1000)
    @Transactional
    public void checkHeartbeats() {
        List<Device> devices = deviceRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Device device : devices) {
            if (device.isOnline() && device.getLastSeen() != null) {
                java.time.Duration silence = java.time.Duration.between(device.getLastSeen(), now);
                if (silence.getSeconds() >= offlineThresholdSeconds) {
                    log.info("checkHeartbeats: marking {} offline (silence={}s, threshold={}s)",
                            device.getDeviceId(), silence.getSeconds(), offlineThresholdSeconds);
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
        String deviceId = device.getDeviceId();
        LocalDateTime lastSeen = device.getLastSeen();
        log.info("broadcastOfflineStatus: sending robot_offline for {} on thread={}",
                deviceId, Thread.currentThread().getName());
        try {
            messagingTemplate.convertAndSend("/topic/status", Map.of(
                "type", "robot_offline",
                "device_id", deviceId,
                "last_seen", lastSeen != null ?
                    lastSeen.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "unknown"
            ));
            log.info("broadcastOfflineStatus: convertAndSend completed for {}", deviceId);
        } catch (Exception e) {
            log.error("broadcastOfflineStatus: convertAndSend failed for {}", deviceId, e);
            throw e;
        }
    }
}
