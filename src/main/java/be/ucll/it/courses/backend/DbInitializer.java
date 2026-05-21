package be.ucll.it.courses.backend;

import be.ucll.it.courses.backend.model.Device;
import be.ucll.it.courses.backend.model.Telemetry;
import be.ucll.it.courses.backend.model.User;
import be.ucll.it.courses.backend.repository.DeviceRepository;
import be.ucll.it.courses.backend.repository.TelemetryRepository;
import be.ucll.it.courses.backend.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class DbInitializer {

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final TelemetryRepository telemetryRepository;
    private final PasswordEncoder passwordEncoder;

    public DbInitializer(UserRepository userRepository, 
                         DeviceRepository deviceRepository, 
                         TelemetryRepository telemetryRepository,
                         PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
        this.telemetryRepository = telemetryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void initializeData() {
        if (userRepository.count() == 0 || 
            userRepository.findAll().stream().anyMatch(u -> u.getPassword() == null || u.getToken() == null)) {
            userRepository.deleteAll();
            deviceRepository.deleteAll();
            telemetryRepository.deleteAll();

            // Users
            User admin = new User("admin", "Administrator", "System", "user-token-admin", passwordEncoder.encode("admin123"), "ADMIN");
            User user1 = new User("jdoe", "John", "Doe", "user-token-01", passwordEncoder.encode("password123"), "VIEWER");
            User user2 = new User("asmith", "Alice", "Smith", "user-token-02", passwordEncoder.encode("password123"), "VIEWER");
            userRepository.saveAll(List.of(admin, user1, user2));

            // Devices
            be.ucll.it.courses.backend.model.Device dev1 = new be.ucll.it.courses.backend.model.Device("ESP32-01", "token-01", "Front Lobby Robot");
            be.ucll.it.courses.backend.model.Device dev2 = new be.ucll.it.courses.backend.model.Device("ESP32-02", "token-02", "Server Room Robot");
            be.ucll.it.courses.backend.model.Device dev3 = new be.ucll.it.courses.backend.model.Device("ESP32-03", "token-03", "Warehouse Robot");
            be.ucll.it.courses.backend.model.Device dev4 = new be.ucll.it.courses.backend.model.Device("ESP32-04", "token-04", "Kitchen Robot");
            deviceRepository.saveAll(List.of(dev1, dev2, dev3, dev4));

            // Telemetry
            Telemetry t1 = new Telemetry();
            t1.setTime(OffsetDateTime.now());
            t1.setBatteryVoltage(12.6f);
            t1.setTemperatureC(22.5f);
            t1.setPumpActive(false);
            telemetryRepository.save(t1);

            System.out.println("Database initialized with " + userRepository.count() + " users, " + deviceRepository.count() + " devices and initial telemetry.");
        } else {
            System.out.println("Database already contains data, skipping initialization.");
        }
    }

    public void clearAll() {
        telemetryRepository.deleteAll();
        deviceRepository.deleteAll();
        userRepository.deleteAll();
        
        userRepository.saveAll(List.of(
            new User("admin",  "Administrator", "",      "user-token-admin", passwordEncoder.encode("admin123"), "ADMIN"),
            new User("jdoe",   "John",          "Doe",   "user-token-01",    passwordEncoder.encode("password123"), "VIEWER"),
            new User("asmith", "Alice",         "Smith", "user-token-02",    passwordEncoder.encode("password123"), "VIEWER")
        ));

        deviceRepository.saveAll(List.of(
            new Device("ESP32-01", "token-01", "Front Lobby Robot"),
            new Device("ESP32-02", "token-02", "Server Room Robot"),
            new Device("ESP32-03", "token-03", "Warehouse Robot"),
            new Device("ESP32-04", "token-04", "Kitchen Robot")
        ));
    }
}
