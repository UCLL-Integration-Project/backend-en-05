package be.ucll.it.courses.backend.controller;

import be.ucll.it.courses.backend.model.User;
import be.ucll.it.courses.backend.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService){this.userService = userService;}

    @GetMapping
    public List<User> getUsers() {
        return userService.getAllUsers();
    }
}
