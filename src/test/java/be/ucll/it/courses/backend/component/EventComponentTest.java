package be.ucll.it.courses.backend.component;

import be.ucll.it.courses.backend.controller.dto.EventRequest;
import be.ucll.it.courses.backend.model.Event;
import be.ucll.it.courses.backend.repository.EventRepository;
import be.ucll.it.courses.backend.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class EventComponentTest {

    @Autowired
    private EventService eventService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private be.ucll.it.courses.backend.repository.DeviceRepository deviceRepository;

    @Test
    void testCreateEventAndDeduplication() {
        String deviceId = "ESP32-01";
        if (deviceRepository.findById(deviceId).isEmpty()) {
            deviceRepository.save(new be.ucll.it.courses.backend.model.Device(deviceId, "token-01", "Test Robot"));
        }

        OffsetDateTime now = OffsetDateTime.now();
        
        // EventRequest(timestamp, temperature, battery_pct, water_level_pct, duration_s, is_extinguished, event_type)
        EventRequest request = new EventRequest(now, 80.5f, (short) 90, (short) 100, 5, false, "fire");

        // 1. Create first event
        eventService.createEvent(request, deviceId);
        
        List<Event> events = eventRepository.findAll();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getTemperature()).isEqualTo(80.5f);

        // 2. Try to create a duplicate within the 5s window
        eventService.createEvent(request, deviceId);
        
        // Should still be only 1 event in DB due to deduplication
        assertThat(eventRepository.findAll()).hasSize(1);

        // 3. Create event outside the window (e.g., 10 seconds later)
        EventRequest requestLater = new EventRequest(now.plusSeconds(10), 81.0f, (short) 89, (short) 99, 5, false, "fire");
        eventService.createEvent(requestLater, deviceId);
        
        assertThat(eventRepository.findAll()).hasSize(2);
    }
}
