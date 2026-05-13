package be.ucll.it.courses.backend.controller;

import be.ucll.it.courses.backend.controller.dto.EventRequest;
import be.ucll.it.courses.backend.controller.dto.EventListResponse;
import be.ucll.it.courses.backend.controller.dto.EventResponse;
import be.ucll.it.courses.backend.service.EventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;
    private final be.ucll.it.courses.backend.repository.DeviceRepository deviceRepository;
    private final be.ucll.it.courses.backend.repository.UserRepository userRepository;

    @Autowired
    public EventController(EventService eventService, 
                           be.ucll.it.courses.backend.repository.DeviceRepository deviceRepository,
                           be.ucll.it.courses.backend.repository.UserRepository userRepository) {
        this.eventService = eventService;
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(
            @RequestHeader(value = "Authorization") String authorization,
            @RequestHeader(value = "X-Device-ID") String deviceId,
            @Valid @RequestBody EventRequest request) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authorization.substring(7);

        // Validate Device ID and Token against DB
        var device = deviceRepository.findByDeviceIdAndDeviceToken(deviceId, token);
        if (device.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        EventResponse response = eventService.createEvent(request, deviceId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<EventListResponse> getEvents(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset,
            HttpServletRequest request) {

        // Accept session cookie or Bearer token
        String token = AuthController.extractTokenFromCookie(request);
        if (token == null && authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
        }

        if (token == null || userRepository.findByToken(token).isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Limit validation
        if (limit > 200) {
            limit = 200;
        }

        OffsetDateTime fromDate = null;
        OffsetDateTime toDate = null;

        try {
            if (from != null) {
                fromDate = OffsetDateTime.parse(from);
            }
            if (to != null) {
                toDate = OffsetDateTime.parse(to);
            }
        } catch (DateTimeParseException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }

        EventListResponse response = eventService.getEvents(fromDate, toDate, limit, offset);
        return ResponseEntity.ok(response);
    }

    // Keep old endpoint for simple fetch if needed, or remove if fully migrating
    @GetMapping("/all")
    public List<be.ucll.it.courses.backend.model.Event> getAllEvents() {
        return eventService.getAllEvents();
    }
}

