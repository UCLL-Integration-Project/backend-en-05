package be.ucll.it.courses.backend.controller;

import be.ucll.it.courses.backend.controller.dto.CreateEventResult;
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
@RequestMapping("/v1/events")
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

        CreateEventResult result = eventService.createEvent(request, deviceId);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return new ResponseEntity<>(result.response(), status);
    }

    @GetMapping
    public ResponseEntity<EventListResponse> getEvents(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) String event_type,
            HttpServletRequest request) {

        String token = AuthController.extractToken(request, authorization);
        if (token == null || userRepository.findByToken(token).isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

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

        EventListResponse response = eventService.getEvents(fromDate, toDate, limit, offset, event_type);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteEvents(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            HttpServletRequest request) {

        String token = AuthController.extractToken(request, authorization);
        if (token == null || userRepository.findByToken(token).isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        eventService.deleteAllEvents();
        return ResponseEntity.noContent().build();
    }

    // Keep old endpoint for simple fetch if needed, or remove if fully migrating
    @GetMapping("/all")
    public List<be.ucll.it.courses.backend.model.Event> getAllEvents() {
        return eventService.getAllEvents();
    }
}

