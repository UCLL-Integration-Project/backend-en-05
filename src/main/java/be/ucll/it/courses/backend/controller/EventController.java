package be.ucll.it.courses.backend.controller;

import be.ucll.it.courses.backend.controller.dto.CreateEventResult;
import be.ucll.it.courses.backend.controller.dto.EventRequest;
import be.ucll.it.courses.backend.controller.dto.EventListResponse;
import be.ucll.it.courses.backend.controller.dto.EventResponse;
import be.ucll.it.courses.backend.exception.UnauthorizedException;
import be.ucll.it.courses.backend.service.EventService;
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

    @Autowired
    public EventController(EventService eventService,
                           be.ucll.it.courses.backend.repository.DeviceRepository deviceRepository) {
        this.eventService = eventService;
        this.deviceRepository = deviceRepository;
    }

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Device-ID", required = false) String deviceId,
            @Valid @RequestBody EventRequest request) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new UnauthorizedException("Invalid or missing Authorization header");
        }

        if (deviceId == null || deviceId.isBlank()) {
            throw new UnauthorizedException("Invalid or missing X-Device-ID header");
        }

        String token = authorization.substring(7);

        var device = deviceRepository.findByDeviceIdAndDeviceToken(deviceId, token);
        if (device.isEmpty()) {
            throw new UnauthorizedException("Device authentication failed");
        }

        CreateEventResult result = eventService.createEvent(request, deviceId);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return new ResponseEntity<>(result.response(), status);
    }

    @GetMapping
    public ResponseEntity<EventListResponse> getEvents(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) String event_type) {

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
    public ResponseEntity<Void> deleteEvents() {
        eventService.deleteAllEvents();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/all")
    public List<be.ucll.it.courses.backend.model.Event> getAllEvents() {
        return eventService.getAllEvents();
    }
}
