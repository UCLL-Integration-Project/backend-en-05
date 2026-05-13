package be.ucll.it.courses.backend.controller;

import be.ucll.it.courses.backend.controller.dto.LoginRequest;
import be.ucll.it.courses.backend.model.User;
import be.ucll.it.courses.backend.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 2 * 60 * 1000L;
    private static final int SESSION_MAX_AGE = 24 * 60 * 60; // 24 hours
    private static final String COOKIE_NAME = "session_token";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final Map<String, Integer> failedAttempts = new ConcurrentHashMap<>();
    private final Map<String, Long> lockoutUntil = new ConcurrentHashMap<>();

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        String username = request.username();

        // Rate limiting check
        if (isLockedOut(username)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("error", "Too many failed attempts. Try again in 1 minutes."));
        }

        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty() || !passwordEncoder.matches(request.password(), userOpt.get().getPassword())) {
            recordFailedAttempt(username);
            int remaining = MAX_ATTEMPTS - failedAttempts.getOrDefault(username, 0);
            if (remaining <= 0) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many failed attempts. Try again in 1 minutes."));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid username or password."));
        }

        // Success — clear failed attempts and set cookie
        failedAttempts.remove(username);
        lockoutUntil.remove(username);

        User user = userOpt.get();
        Cookie cookie = new Cookie(COOKIE_NAME, user.getToken());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(SESSION_MAX_AGE);
        response.addCookie(cookie);

        return ResponseEntity.ok(Map.of(
            "username", user.getUsername(),
            "firstName", user.getFirstName(),
            "lastName", user.getLastName(),
            "token", user.getToken()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully."));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        String token = extractTokenFromCookie(request);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated."));
        }

        return userRepository.findByToken(token)
            .<ResponseEntity<?>>map(user -> ResponseEntity.ok(Map.of(
                "username", user.getUsername(),
                "firstName", user.getFirstName(),
                "lastName", user.getLastName(),
                "token", user.getToken()
            )))
            .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Session invalid.")));
    }

    public static String extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
            .filter(c -> COOKIE_NAME.equals(c.getName()))
            .map(Cookie::getValue)
            .findFirst()
            .orElse(null);
    }

    private boolean isLockedOut(String username) {
        Long until = lockoutUntil.get(username);
        if (until == null) return false;
        if (System.currentTimeMillis() > until) {
            lockoutUntil.remove(username);
            failedAttempts.remove(username);
            return false;
        }
        return true;
    }

    private void recordFailedAttempt(String username) {
        int attempts = failedAttempts.merge(username, 1, Integer::sum);
        if (attempts >= MAX_ATTEMPTS) {
            lockoutUntil.put(username, System.currentTimeMillis() + LOCKOUT_DURATION_MS);
        }
    }
}
