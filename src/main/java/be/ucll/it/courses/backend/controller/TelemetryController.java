package be.ucll.it.courses.backend.controller;

import be.ucll.it.courses.backend.exception.UnauthorizedException;
import be.ucll.it.courses.backend.model.Telemetry;
import be.ucll.it.courses.backend.repository.TelemetryRepository;
import be.ucll.it.courses.backend.repository.DeviceRepository;
import be.ucll.it.courses.backend.service.LocationBroadcastService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/v1/telemetry")
public class TelemetryController {

    private final TelemetryRepository telemetryRepository;
    private final DeviceRepository deviceRepository;
    private final LocationBroadcastService locationBroadcastService;

    @Autowired
    public TelemetryController(TelemetryRepository telemetryRepository, DeviceRepository deviceRepository,
                               LocationBroadcastService locationBroadcastService) {
        this.telemetryRepository = telemetryRepository;
        this.deviceRepository = deviceRepository;
        this.locationBroadcastService = locationBroadcastService;
    }

    @PostMapping
    public ResponseEntity<Void> postTelemetry(
            @RequestHeader(value = "Authorization") String authorization,
            @RequestHeader(value = "X-Device-ID") String deviceId,
            @RequestBody Telemetry telemetry) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new UnauthorizedException("Invalid or missing Authorization header");
        }

        String token = authorization.substring(7);

        // Validate Device ID and Token against DB
        var device = deviceRepository.findByDeviceIdAndDeviceToken(deviceId, token);
        if (device.isEmpty()) {
            throw new UnauthorizedException("Device authentication failed");
        }

        telemetry.setTime(OffsetDateTime.now());
        telemetryRepository.save(telemetry);
        locationBroadcastService.broadcastIfLocationPresent(telemetry);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
