package be.ucll.it.courses.backend.controller;

import be.ucll.it.courses.backend.repository.DeviceRepository;
import be.ucll.it.courses.backend.service.RobotStatusService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/robot")
public class RobotController {

    private final RobotStatusService robotStatusService;
    private final DeviceRepository deviceRepository;

    public RobotController(RobotStatusService robotStatusService, DeviceRepository deviceRepository) {
        this.robotStatusService = robotStatusService;
        this.deviceRepository = deviceRepository;
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<?> heartbeat(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("X-Device-ID") String deviceId) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authorization.substring(7);
        if (deviceRepository.findByDeviceIdAndDeviceToken(deviceId, token).isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        robotStatusService.recordHeartbeat(deviceId);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(robotStatusService.getStatus());
    }
}
