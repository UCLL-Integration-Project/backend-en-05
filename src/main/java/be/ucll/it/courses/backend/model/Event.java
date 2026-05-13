package be.ucll.it.courses.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "fire_events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "incident_id")
    private UUID incidentId;

    @Column(name = "timestamp")
    private OffsetDateTime timestamp;

    @Column(name = "temperature")
    private Float temperature;

    @Column(name = "battery_pct")
    private Short batteryPct;

    @Column(name = "duration_s")
    private Integer durationS;

    @Column(name = "is_extinguished")
    private Boolean isExtinguished;

    @Column(name = "device_id")
    private String deviceId;

    public Event() {}

    public UUID getIncidentId() { return incidentId; }
    public void setIncidentId(UUID incidentId) { this.incidentId = incidentId; }

    public OffsetDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; }

    public Float getTemperature() { return temperature; }
    public void setTemperature(Float temperature) { this.temperature = temperature; }

    public Short getBatteryPct() { return batteryPct; }
    public void setBatteryPct(Short batteryPct) { this.batteryPct = batteryPct; }

    public Integer getDurationS() { return durationS; }
    public void setDurationS(Integer durationS) { this.durationS = durationS; }

    public Boolean getIsExtinguished() { return isExtinguished; }
    public void setIsExtinguished(Boolean isExtinguished) { this.isExtinguished = isExtinguished; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
}
