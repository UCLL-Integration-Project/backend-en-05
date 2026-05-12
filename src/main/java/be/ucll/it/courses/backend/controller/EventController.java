package be.ucll.it.courses.backend.controller;

import be.ucll.it.courses.backend.controller.dto.EventRequest;
import be.ucll.it.courses.backend.controller.dto.EventResponse;
import be.ucll.it.courses.backend.service.EventService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;
    private final be.ucll.it.courses.backend.repository.DeviceRepository deviceRepository;

    @Autowired
    public EventController(EventService eventService, be.ucll.it.courses.backend.repository.DeviceRepository deviceRepository) {
        this.eventService = eventService;
        this.deviceRepository = deviceRepository;
    }

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(
            @RequestHeader(value = "Authorization") String authorization,
            @RequestHeader(value = "X-Device-ID") String deviceId,
            @Valid @RequestBody EventRequest request) {

        System.out.println("Received request from Device: " + deviceId);
        
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            System.out.println("Auth header missing or invalid format");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authorization.substring(7);
        System.out.println("Extracted Token: " + token);

        // Validate Device ID and Token against DB
        var device = deviceRepository.findByDeviceIdAndDeviceToken(deviceId, token);
        if (device.isEmpty()) {
            System.out.println("Device NOT found in DB with ID: " + deviceId + " and Token: " + token);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        System.out.println("Device validated: " + device.get().getName());
        EventResponse response = eventService.createEvent(request, deviceId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @org.springframework.web.bind.annotation.GetMapping
    public java.util.List<be.ucll.it.courses.backend.model.Event> getEvents() {
        return eventService.getAllEvents();
    }
}

