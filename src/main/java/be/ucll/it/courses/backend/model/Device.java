package be.ucll.it.courses.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "devices")
public class Device {

    @Id
    @jakarta.persistence.Column(name = "device_id")
    private String deviceId;

    @jakarta.persistence.Column(name = "device_token")
    private String deviceToken;

    @jakarta.persistence.Column(name = "name")
    private String name;

    public Device() {}

    public Device(String deviceId, String deviceToken, String name) {
        this.deviceId = deviceId;
        this.deviceToken = deviceToken;
        this.name = name;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
