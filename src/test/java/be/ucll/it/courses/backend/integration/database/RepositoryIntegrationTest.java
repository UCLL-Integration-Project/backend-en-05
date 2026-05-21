package be.ucll.it.courses.backend.integration.database;

import be.ucll.it.courses.backend.integration.BaseIntegrationTest;
import be.ucll.it.courses.backend.model.User;
import be.ucll.it.courses.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class RepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByUsernameReturnsUser() {
        User user = new User();
        user.setUsername("dbtest");
        user.setRole("VIEWER");
        userRepository.save(user);

        Optional<User> found = userRepository.findByUsername("dbtest");

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("dbtest");
    }
}
