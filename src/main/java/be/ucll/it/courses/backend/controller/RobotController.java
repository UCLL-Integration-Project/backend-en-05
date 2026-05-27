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
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("X-Device-ID") String deviceId) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new UnauthorizedException("Invalid or missing Authorization header");
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
