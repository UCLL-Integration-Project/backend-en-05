package be.ucll.it.courses.backend.unit.service;

import be.ucll.it.courses.backend.controller.dto.EventRequest;
import be.ucll.it.courses.backend.controller.dto.EventResponse;
import be.ucll.it.courses.backend.model.Event;
import be.ucll.it.courses.backend.repository.EventRepository;
import be.ucll.it.courses.backend.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EventServiceTest {

    private EventService eventService;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        eventService = new EventService(eventRepository, messagingTemplate);
    }

    @Test
    void createEventSavesToRepositoryAndBroadcasts() {
        EventRequest request = new EventRequest(
            OffsetDateTime.now(),
            25.0f,
            (short) 80,
            10,
            true
        );
        String deviceId = "test-device";
        UUID incidentId = UUID.randomUUID();

        Event savedEvent = new Event();
        savedEvent.setIncidentId(incidentId);
        savedEvent.setTimestamp(request.timestamp());
        savedEvent.setIsExtinguished(request.is_extinguished());

        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        EventResponse response = eventService.createEvent(request, deviceId);

        assertThat(response.incident_id()).isEqualTo(incidentId);
        assertThat(response.status()).isEqualTo("created");

        verify(eventRepository).save(any(Event.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/events"), any(Object.class));
    }
}
