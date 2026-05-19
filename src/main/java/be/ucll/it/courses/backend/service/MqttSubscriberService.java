package be.ucll.it.courses.backend.service;

import be.ucll.it.courses.backend.model.Telemetry;
import be.ucll.it.courses.backend.repository.TelemetryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;

@Service
public class MqttSubscriberService {

    private static final Logger logger = LoggerFactory.getLogger(MqttSubscriberService.class);
    private final TelemetryRepository telemetryRepository;
    private final ObjectMapper objectMapper;

    public MqttSubscriberService(TelemetryRepository telemetryRepository, ObjectMapper objectMapper) {
        this.telemetryRepository = telemetryRepository;
        this.objectMapper = objectMapper;
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "mqtt.enabled", havingValue = "true")
    public MessageHandler handler() {
        return message -> {
            try {
                String payload = (String) message.getPayload();
                logger.debug("Received MQTT payload: {}", payload);
                
                Map<String, Object> data = objectMapper.readValue(payload, Map.class);
                
                Telemetry telemetry = new Telemetry();
                telemetry.setTime(OffsetDateTime.now());
                
                if (data.containsKey("temperature")) {
                    telemetry.setTemperatureC(((Number) data.get("temperature")).floatValue());
                }
                
                telemetryRepository.save(telemetry);
                logger.info("Saved telemetry from MQTT message");
                
            } catch (Exception e) {
                logger.error("Error processing MQTT message", e);
                throw new RuntimeException("Failed to process MQTT message", e);
            }
        };
    }
}
