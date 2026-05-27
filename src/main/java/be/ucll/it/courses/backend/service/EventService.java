package be.ucll.it.courses.backend.service;

import be.ucll.it.courses.backend.controller.dto.CreateEventResult;
import be.ucll.it.courses.backend.controller.dto.EventListItemResponse;
import be.ucll.it.courses.backend.controller.dto.EventListResponse;
import be.ucll.it.courses.backend.controller.dto.EventRequest;
import be.ucll.it.courses.backend.controller.dto.EventResponse;
import be.ucll.it.courses.backend.model.Event;
import be.ucll.it.courses.backend.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private boolean lowWaterAlertActive = false;

    @Value("${app.deduplication.window-seconds:5}")
    private long deduplicationWindowSeconds;

    @Autowired
    public EventService(EventRepository eventRepository, SimpMessagingTemplate messagingTemplate) {
        this.eventRepository = eventRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public EventListResponse getEvents(OffsetDateTime from, OffsetDateTime to, int limit, int offset, String eventType) {
        Specification<Event> spec = Specification.where(null);

        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("timestamp"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("timestamp"), to));
        }
        if (eventType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("eventType"), eventType));
        }

        int page = offset / limit;
        Page<Event> eventPage = eventRepository.findAll(spec, PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "timestamp")));

        List<EventListItemResponse> eventList = eventPage.getContent().stream()
            .map(e -> new EventListItemResponse(
                e.getIncidentId(),
                e.getTimestamp(),
                e.getTemperature(),
                e.getBatteryPct(),
                e.getDurationS(),
                e.getIsExtinguished(),
                e.getLatitude(),
                e.getLongitude(),
                e.getEventType(),
                e.getWaterLevelPct()
            ))
            .collect(Collectors.toList());

        return new EventListResponse(eventPage.getTotalElements(), eventList);
    }

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public void deleteAllEvents() {
        eventRepository.deleteAll();
    }

    public CreateEventResult createEvent(EventRequest request, String deviceId) {
        OffsetDateTime windowStart = request.timestamp().minusSeconds(deduplicationWindowSeconds);
        OffsetDateTime windowEnd = request.timestamp().plusSeconds(deduplicationWindowSeconds);

        Optional<Event> existing = eventRepository.findFirstByDeviceIdAndTimestampBetween(deviceId, windowStart, windowEnd);
        if (existing.isPresent()) {
            return new CreateEventResult(new EventResponse(existing.get().getIncidentId(), "duplicate"), false);
        }

        Event event = new Event();
        event.setTimestamp(request.timestamp());
        event.setTemperature(request.temperature());
        event.setBatteryPct(request.battery_pct());
        event.setDurationS(request.duration_s());
        event.setIsExtinguished(request.is_extinguished());
        event.setLatitude(request.latitude());
        event.setLongitude(request.longitude());
        event.setLocationAccuracyM(request.locationAccuracy_m());
        event.setWaterLevelPct(request.water_level_pct());
        event.setDeviceId(deviceId);
        event.setEventType(request.event_type());

        Event savedEvent = eventRepository.save(event);

        messagingTemplate.convertAndSend("/topic/events", new EventListItemResponse(
            savedEvent.getIncidentId(),
            savedEvent.getTimestamp(),
            savedEvent.getTemperature(),
            savedEvent.getBatteryPct(),
            savedEvent.getDurationS(),
            savedEvent.getIsExtinguished(),
            savedEvent.getLatitude(),
            savedEvent.getLongitude(),
            savedEvent.getEventType(),
            savedEvent.getWaterLevelPct()
        ));

        // Low Water Alert Logic
        if ("low_water".equals(request.event_type()) && !lowWaterAlertActive) {
            lowWaterAlertActive = true;
            java.util.Map<String, Object> alert = new java.util.HashMap<>();
            alert.put("type", "low_water_warning");
            alert.put("water_level_pct", request.water_level_pct());
            alert.put("timestamp", request.timestamp().toString());
            messagingTemplate.convertAndSend("/topic/alerts", alert);
        } else if (lowWaterAlertActive && request.water_level_pct() != null && request.water_level_pct() > 25) {
            lowWaterAlertActive = false;
            messagingTemplate.convertAndSend("/topic/alerts", java.util.Map.of(
                "type", "low_water_cleared"
            ));
        }

        return new CreateEventResult(new EventResponse(savedEvent.getIncidentId(), "created"), true);
    }
}
