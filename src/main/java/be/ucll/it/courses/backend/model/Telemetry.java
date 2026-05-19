package be.ucll.it.courses.backend.model;

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

    @Column(name = "time")
    private OffsetDateTime time;

    @Column(name = "battery_voltage")
    private Float batteryVoltage;

    @Column(name = "temperature_c")
    private Float temperatureC;

    @Column(name = "flame_left")
    private Short flameLeft;

    @Column(name = "flame_center")
    private Short flameCenter;

    @Column(name = "flame_right")
    private Short flameRight;

    @Column(name = "pump_active")
    private Boolean pumpActive = false;

    @Column(name = "motor_left_pwm")
    private Short motorLeftPwm;

    @Column(name = "motor_right_pwm")
    private Short motorRightPwm;

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
}
