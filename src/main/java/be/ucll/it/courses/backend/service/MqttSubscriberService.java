package be.ucll.it.courses.backend.service;

import be.ucll.it.courses.backend.model.Telemetry;
import be.ucll.it.courses.backend.repository.TelemetryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;

@Service
public class MqttSubscriberService {

    private final TelemetryRepository telemetryRepository;
    private final ObjectMapper objectMapper;

    public MqttSubscriberService(TelemetryRepository telemetryRepository, ObjectMapper objectMapper) {
        this.telemetryRepository = telemetryRepository;
        this.objectMapper = objectMapper;
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public MessageHandler handler() {
        return message -> {
            try {
                String payload = (String) message.getPayload();
                Map<String, Object> data = objectMapper.readValue(payload, Map.class);
                
                Telemetry telemetry = new Telemetry();
                telemetry.setTime(OffsetDateTime.now());
                
                if (data.containsKey("battery_pct")) {
                    // Mapping battery_pct to something in Telemetry or just ensuring it's in the status
                    // Our Telemetry model seems to have batteryVoltage but not pct. 
                    // Let's assume we use what we have or just store it.
                }
                
                if (data.containsKey("temperature")) {
                    telemetry.setTemperatureC(((Number) data.get("temperature")).floatValue());
                }
                
                telemetryRepository.save(telemetry);
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }
}
