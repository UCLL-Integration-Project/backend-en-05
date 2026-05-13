package be.ucll.it.courses.backend;

import be.ucll.it.courses.backend.model.Device;
import be.ucll.it.courses.backend.model.User;
import be.ucll.it.courses.backend.repository.DeviceRepository;
import be.ucll.it.courses.backend.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile({"local", "dev"})
public class DbInitializer {

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final PasswordEncoder passwordEncoder;

    public DbInitializer(UserRepository userRepository, DeviceRepository deviceRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void initializeData() {
        if (userRepository.count() == 0 || userRepository.findAll().stream().anyMatch(u -> u.getPassword() == null)) {
            userRepository.deleteAll();
            deviceRepository.deleteAll();

            // Users
            User admin = new User("admin", "Administrator", "System", "user-token-admin", passwordEncoder.encode("admin123"));
            User user1 = new User("jdoe", "John", "Doe", "user-token-01", passwordEncoder.encode("password123"));
            User user2 = new User("asmith", "Alice", "Smith", "user-token-02", passwordEncoder.encode("password123"));
            userRepository.saveAll(List.of(admin, user1, user2));

            // Devices
            be.ucll.it.courses.backend.model.Device dev1 = new be.ucll.it.courses.backend.model.Device("ESP32-01", "token-01", "Front Lobby Robot");
            be.ucll.it.courses.backend.model.Device dev2 = new be.ucll.it.courses.backend.model.Device("ESP32-02", "token-02", "Server Room Robot");
            be.ucll.it.courses.backend.model.Device dev3 = new be.ucll.it.courses.backend.model.Device("ESP32-03", "token-03", "Warehouse Robot");
            be.ucll.it.courses.backend.model.Device dev4 = new be.ucll.it.courses.backend.model.Device("ESP32-04", "token-04", "Kitchen Robot");
            deviceRepository.saveAll(List.of(dev1, dev2, dev3, dev4));

            System.out.println("Database initialized with " + userRepository.count() + " users and " + deviceRepository.count() + " devices.");
        } else {
            System.out.println("Database already contains data, skipping initialization.");
        }
    }

    public void clearAll() {
        deviceRepository.deleteAll();
        userRepository.deleteAll();
        if (userRepository.count() == 0) {
            userRepository.saveAll(List.of(
                new User("admin",  "Administrator", "",      "user-token-admin", passwordEncoder.encode("admin123")),
                new User("jdoe",   "John",          "Doe",   "user-token-01",    passwordEncoder.encode("password123")),
                new User("asmith", "Alice",         "Smith", "user-token-02",    passwordEncoder.encode("password123"))
            ));
        }

        if (deviceRepository.count() == 0) {
            deviceRepository.saveAll(List.of(
                new Device("ESP32-01", "token-01", "Front Lobby Robot"),
                new Device("ESP32-02", "token-02", "Server Room Robot"),
                new Device("ESP32-03", "token-03", "Warehouse Robot"),
                new Device("ESP32-04", "token-04", "Kitchen Robot")
            ));
        }
    }
}
