package be.ucll.it.courses.backend.unit.service;

import be.ucll.it.courses.backend.controller.dto.CreateEventResult;
import be.ucll.it.courses.backend.controller.dto.EventRequest;
import be.ucll.it.courses.backend.model.Event;
import be.ucll.it.courses.backend.repository.EventRepository;
import be.ucll.it.courses.backend.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;
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
        ReflectionTestUtils.setField(eventService, "deduplicationWindowSeconds", 5L);
    }

    @Test
    void createEventSavesToRepositoryAndBroadcastsWhenNoDuplicate() {
        EventRequest request = new EventRequest(
            OffsetDateTime.now(),
            25.0f,
            (short) 80,
            (short) 50,
            10,
            true,
            "fire"
        );
        String deviceId = "test-device";
        UUID incidentId = UUID.randomUUID();

        when(eventRepository.findFirstByDeviceIdAndTimestampBetween(any(), any(), any()))
            .thenReturn(Optional.empty());

        Event savedEvent = new Event();
        savedEvent.setIncidentId(incidentId);
        savedEvent.setTimestamp(request.timestamp());
        savedEvent.setIsExtinguished(request.is_extinguished());
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        CreateEventResult result = eventService.createEvent(request, deviceId);

        assertThat(result.created()).isTrue();
        assertThat(result.response().incident_id()).isEqualTo(incidentId);
        assertThat(result.response().status()).isEqualTo("created");
        verify(eventRepository).save(any(Event.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/events"), any(Object.class));
    }

    @Test
    void createEventReturnsDuplicateWhenEventExistsWithinWindow() {
        OffsetDateTime now = OffsetDateTime.now();
        EventRequest request = new EventRequest(now, 25.0f, (short) 80, (short) 50, 10, true, "fire");
        String deviceId = "test-device";
        UUID existingId = UUID.randomUUID();

        Event existing = new Event();
        existing.setIncidentId(existingId);
        existing.setDeviceId(deviceId);
        existing.setTimestamp(now.minusSeconds(2));

        when(eventRepository.findFirstByDeviceIdAndTimestampBetween(any(), any(), any()))
            .thenReturn(Optional.of(existing));

        CreateEventResult result = eventService.createEvent(request, deviceId);

        assertThat(result.created()).isFalse();
        assertThat(result.response().incident_id()).isEqualTo(existingId);
        assertThat(result.response().status()).isEqualTo("duplicate");
        verify(eventRepository, never()).save(any(Event.class));
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void createEventCreatesNewRecordWhenOutsideDeduplicationWindow() {
        OffsetDateTime now = OffsetDateTime.now();
        EventRequest request = new EventRequest(now, 25.0f, (short) 80, (short) 50, 10, true, "fire");
        String deviceId = "test-device";
        UUID newId = UUID.randomUUID();

        when(eventRepository.findFirstByDeviceIdAndTimestampBetween(any(), any(), any()))
            .thenReturn(Optional.empty());

        Event savedEvent = new Event();
        savedEvent.setIncidentId(newId);
        savedEvent.setTimestamp(now);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        CreateEventResult result = eventService.createEvent(request, deviceId);

        assertThat(result.created()).isTrue();
        assertThat(result.response().status()).isEqualTo("created");
        verify(eventRepository).save(any(Event.class));
    }
}
