package be.ucll.it.courses.backend;

import be.ucll.it.courses.backend.model.User;
import be.ucll.it.courses.backend.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DbInitializer {

    private final UserRepository userRepository;

    public DbInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void initializeData() {
        clearAll();

        User user1 = new User("jdoe", "John", "Doe");
        User user2 = new User("asmith", "Alice", "Smith");
        User user3 = new User("bjones", "Bob", "Jones");
        User user4 = new User("emartinez", "Elena", "Martinez");

        userRepository.saveAll(List.of(user1, user2, user3, user4));

        System.out.println("Database initialized with " + userRepository.count() + " users.");
    }

    public void clearAll() {
        userRepository.deleteAll();
    }
}