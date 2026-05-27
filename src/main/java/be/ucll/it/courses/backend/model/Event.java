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

    @Column(name = "water_level_pct")
    private Short waterLevelPct;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "event_type")
    private String eventType;

    @Column(name="latitude")
    private Double latitude;

    @Column(name="longitude")
    private Double longitude;

    @Column(name="location_accuracy_m")
    private Integer locationAccuracyM;


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

    public Short getWaterLevelPct() { return waterLevelPct; }
    public void setWaterLevelPct(Short waterLevelPct) { this.waterLevelPct = waterLevelPct; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Integer getLocationAccuracyM() { return locationAccuracyM; }
    public void setLocationAccuracyM(Integer locationAccuracyM) {this.locationAccuracyM=locationAccuracyM;}
}
