package be.ucll.it.courses.backend.controller;

import be.ucll.it.courses.backend.exception.UnauthorizedException;
import be.ucll.it.courses.backend.repository.DeviceRepository;
import be.ucll.it.courses.backend.service.RobotStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/robot")
public class RobotController {

    private final RobotStatusService robotStatusService;
    private final DeviceRepository deviceRepository;

    public RobotController(RobotStatusService robotStatusService, DeviceRepository deviceRepository) {
        this.robotStatusService = robotStatusService;
        this.deviceRepository = deviceRepository;
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<?> heartbeat(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Device-ID", required = false) String deviceId) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new UnauthorizedException("Invalid or missing Authorization header");
        }

        if (deviceId == null || deviceId.isBlank()) {
            throw new UnauthorizedException("Invalid or missing X-Device-ID header");
        }

        String token = authorization.substring(7);
        if (deviceRepository.findByDeviceIdAndDeviceToken(deviceId, token).isEmpty()) {
            throw new UnauthorizedException("Device authentication failed");
        }

        robotStatusService.recordHeartbeat(deviceId);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(robotStatusService.getStatus());
    }
}
