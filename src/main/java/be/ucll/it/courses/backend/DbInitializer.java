package be.ucll.it.courses.backend;

import be.ucll.it.courses.backend.repository.UserRepository;

public class DbInitializer {


    private final UserRepository userRepository;

    public DbInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void clearAll() {
        userRepository.deleteAll();
    }

}
