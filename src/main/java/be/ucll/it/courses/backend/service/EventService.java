package be.ucll.it.courses.backend.service;

import be.ucll.it.courses.backend.controller.dto.EventListItemResponse;
import be.ucll.it.courses.backend.controller.dto.EventListResponse;
import be.ucll.it.courses.backend.controller.dto.EventRequest;
import be.ucll.it.courses.backend.controller.dto.EventResponse;
import be.ucll.it.courses.backend.model.Event;
import be.ucll.it.courses.backend.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final SimpMessagingTemplate messagingTemplate;

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
                e.getEventType()
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

    public EventResponse createEvent(EventRequest request, String deviceId) {
        Event event = new Event();
        event.setTimestamp(request.timestamp());
        event.setTemperature(request.temperature());
        event.setBatteryPct(request.battery_pct());
        event.setDurationS(request.duration_s());
        event.setIsExtinguished(request.is_extinguished());
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
            savedEvent.getEventType()
        ));

        return new EventResponse(savedEvent.getIncidentId(), "created");
    }
}
