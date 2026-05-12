package be.ucll.it.courses.backend.service;

import be.ucll.it.courses.backend.controller.dto.EventRequest;
import be.ucll.it.courses.backend.controller.dto.EventResponse;
import be.ucll.it.courses.backend.model.Event;
import be.ucll.it.courses.backend.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

import be.ucll.it.courses.backend.controller.dto.EventListItemResponse;
import be.ucll.it.courses.backend.controller.dto.EventListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import java.time.OffsetDateTime;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;

    @Autowired
    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public EventListResponse getEvents(OffsetDateTime from, OffsetDateTime to, int limit, int offset) {
        Specification<Event> spec = (root, query, cb) -> cb.conjunction();

        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("timestamp"), from));
        }

        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("timestamp"), to));
        }

        // JPA PageRequest uses page index, not offset. 
        // We'll use a simple conversion: page = offset / limit
        int page = offset / limit;
        PageRequest pageRequest = PageRequest.of(page, limit);

        Page<Event> eventPage = eventRepository.findAll(spec, pageRequest);

        List<EventListItemResponse> eventList = eventPage.getContent().stream()
            .map(e -> new EventListItemResponse(
                e.getIncidentId(),
                e.getTimestamp(),
                e.getTemperature(),
                e.getBatteryPct(),
                e.getDurationS(),
                e.getIsExtinguished()
            ))
            .collect(Collectors.toList());

        return new EventListResponse(eventPage.getTotalElements(), eventList);
    }

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public EventResponse createEvent(EventRequest request, String deviceId) {
        Event event = new Event();
        event.setTimestamp(request.timestamp());
        event.setTemperature(request.temperature());
        event.setBatteryPct(request.battery_pct());
        event.setDurationS(request.duration_s());
        event.setIsExtinguished(request.is_extinguished());
        event.setDeviceId(deviceId);

        Event savedEvent = eventRepository.save(event);

        return new EventResponse(savedEvent.getIncidentId(), "created");
    }
}
