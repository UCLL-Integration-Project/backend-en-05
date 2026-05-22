package be.ucll.it.courses.backend.controller.dto;

public class RobotTelemetryMessage {
    private String deviceId;
    private float temperature;
    private float batteryLevel;
    private boolean fireDetected;

    public RobotTelemetryMessage() {}

    public RobotTelemetryMessage(String deviceId, float temperature, float batteryLevel, boolean fireDetected) {
        this.deviceId = deviceId;
        this.temperature = temperature;
        this.batteryLevel = batteryLevel;
        this.fireDetected = fireDetected;
    }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public float getTemperature() { return temperature; }
    public void setTemperature(float temperature) { this.temperature = temperature; }
    public float getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(float batteryLevel) { this.batteryLevel = batteryLevel; }
    public boolean isFireDetected() { return fireDetected; }
    public void setFireDetected(boolean fireDetected) { this.fireDetected = fireDetected; }
}
