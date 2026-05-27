package be.ucll.it.courses.backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "telemetry")
public class Telemetry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonProperty("time")
    @Column(name = "time")
    private OffsetDateTime time;

    @JsonProperty("battery_voltage")
    @Column(name = "battery_voltage")
    private Float batteryVoltage;

    @JsonProperty("temperature_c")
    @Column(name = "temperature_c")
    private Float temperatureC;

    @JsonProperty("flame_left")
    @Column(name = "flame_left")
    private Short flameLeft;

    @JsonProperty("flame_center")
    @Column(name = "flame_center")
    private Short flameCenter;

    @JsonProperty("flame_right")
    @Column(name = "flame_right")
    private Short flameRight;

    @JsonProperty("pump_active")
    @Column(name = "pump_active")
    private Boolean pumpActive = false;

    @JsonProperty("motor_left_pwm")
    @Column(name = "motor_left_pwm")
    private Short motorLeftPwm;

    @JsonProperty("motor_right_pwm")
    @Column(name = "motor_right_pwm")
    private Short motorRightPwm;

    @JsonProperty("water_level_pct")
    @Column(name = "water_level_pct")
    private Short waterLevelPct;

    @JsonProperty("latitude")
    @Column(name = "latitude")
    private Double latitude;

    @JsonProperty("longitude")
    @Column(name = "longitude")
    private Double longitude;

    @JsonProperty("accuracy_m")
    @Column(name = "accuracy_m")
    private Double accuracyM;

    public Telemetry() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public OffsetDateTime getTime() { return time; }
    public void setTime(OffsetDateTime time) { this.time = time; }

    public Float getBatteryVoltage() { return batteryVoltage; }
    public void setBatteryVoltage(Float batteryVoltage) { this.batteryVoltage = batteryVoltage; }

    public Float getTemperatureC() { return temperatureC; }
    public void setTemperatureC(Float temperatureC) { this.temperatureC = temperatureC; }

    public Short getFlameLeft() { return flameLeft; }
    public void setFlameLeft(Short flameLeft) { this.flameLeft = flameLeft; }

    public Short getFlameCenter() { return flameCenter; }
    public void setFlameCenter(Short flameCenter) { this.flameCenter = flameCenter; }

    public Short getFlameRight() { return flameRight; }
    public void setFlameRight(Short flameRight) { this.flameRight = flameRight; }

    public Boolean getPumpActive() { return pumpActive; }
    public void setPumpActive(Boolean pumpActive) { this.pumpActive = pumpActive; }

    public Short getMotorLeftPwm() { return motorLeftPwm; }
    public void setMotorLeftPwm(Short motorLeftPwm) { this.motorLeftPwm = motorLeftPwm; }

    public Short getMotorRightPwm() { return motorRightPwm; }
    public void setMotorRightPwm(Short motorRightPwm) { this.motorRightPwm = motorRightPwm; }

    public Short getWaterLevelPct() { return waterLevelPct; }
    public void setWaterLevelPct(Short waterLevelPct) { this.waterLevelPct = waterLevelPct; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getAccuracyM() { return accuracyM; }
    public void setAccuracyM(Double accuracyM) { this.accuracyM = accuracyM; }
}
