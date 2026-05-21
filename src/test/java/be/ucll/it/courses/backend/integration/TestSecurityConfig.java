package be.ucll.it.courses.backend.integration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@TestConfiguration
public class TestSecurityConfig {

    @Bean
    public JwtDecoder jwtDecoder() {
        return token -> Jwt.withTokenValue(token)
                .header("alg", "none")
                .subject(token)
                .claim("email", "test@example.com")
                .claim("name", "Test User")
                .claim("https://en05.ucll.be/roles", List.of(token))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}
