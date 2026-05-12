package be.ucll.it.courses.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @jakarta.persistence.Column(name = "incident_id")
    private UUID incidentId;

    @jakarta.persistence.Column(name = "timestamp")
    private OffsetDateTime timestamp;

    @jakarta.persistence.Column(name = "temperature")
    private Float temperature;

    @jakarta.persistence.Column(name = "battery_pct")
    private Integer batteryPct;

    @jakarta.persistence.Column(name = "duration_s")
    private Integer durationS;

    @jakarta.persistence.Column(name = "is_extinguished")
    private Boolean isExtinguished;

    @jakarta.persistence.Column(name = "device_id")
    private String deviceId;

    public Event() {}

    public UUID getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(UUID incidentId) {
        this.incidentId = incidentId;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Float getTemperature() {
        return temperature;
    }

    public void setTemperature(Float temperature) {
        this.temperature = temperature;
    }

    public Integer getBatteryPct() {
        return batteryPct;
    }

    public void setBatteryPct(Integer batteryPct) {
        this.batteryPct = batteryPct;
    }

    public Integer getDurationS() {
        return durationS;
    }

    public void setDurationS(Integer durationS) {
        this.durationS = durationS;
    }

    public Boolean getIsExtinguished() {
        return isExtinguished;
    }

    public void setIsExtinguished(Boolean extinguished) {
        isExtinguished = extinguished;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
