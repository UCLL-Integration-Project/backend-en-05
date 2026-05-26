package be.ucll.it.courses.backend.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RobotTelemetryMessage {
    @JsonProperty("device_id")
    private String deviceId;
    
    @JsonProperty("battery_pct")
    private short batteryPct;
    
    @JsonProperty("water_level_pct")
    private short waterLevelPct;
    
    private Double latitude;
    private Double longitude;
    private String timestamp;
    
    @JsonProperty("fire_detected")
    private boolean fireDetected;

    public RobotTelemetryMessage() {}

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    
    public short getBatteryPct() { return batteryPct; }
    public void setBatteryPct(short batteryPct) { this.batteryPct = batteryPct; }
    
    public short getWaterLevelPct() { return waterLevelPct; }
    public void setWaterLevelPct(short waterLevelPct) { this.waterLevelPct = waterLevelPct; }
    
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    
    public boolean isFireDetected() { return fireDetected; }
    public void setFireDetected(boolean fireDetected) { this.fireDetected = fireDetected; }
}
