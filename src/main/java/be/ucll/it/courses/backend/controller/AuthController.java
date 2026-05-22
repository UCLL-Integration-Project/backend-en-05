package be.ucll.it.courses.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private static final String ROLES_CLAIM = "https://en05.ucll.be/roles";

    /**
     * Returns the currently authenticated user's info from their JWT.
     * Auth0 handles login/logout — we only need this endpoint to tell
     * the frontend who the user is and what role they have.
     */
    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> body = new HashMap<>();
        body.put("sub", jwt.getSubject());
        body.put("email", jwt.getClaimAsString("email"));
        body.put("name", jwt.getClaimAsString("name"));

        List<String> roles = jwt.getClaimAsStringList(ROLES_CLAIM);
        body.put("roles", roles != null ? roles : List.of());

        return ResponseEntity.ok(body);
    }
}
