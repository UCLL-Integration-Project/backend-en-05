package be.ucll.it.courses.backend.controller;

import be.ucll.it.courses.backend.controller.dto.HealthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

@RestController
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @Value("${project.version:0.0.1-SNAPSHOT}")
    private String version;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        long uptimeS = TimeUnit.MILLISECONDS.toSeconds(uptimeMs);

        String dbStatus = "connected";
        HttpStatus httpStatus = HttpStatus.OK;

        try {
            jdbcTemplate.execute("SELECT 1");
        } catch (Exception e) {
            dbStatus = "unreachable";
            httpStatus = HttpStatus.SERVICE_UNAVAILABLE;
        }

        HealthResponse response = new HealthResponse(
                httpStatus == HttpStatus.OK ? "ok" : "degraded",
                version,
                uptimeS,
                dbStatus
        );

        return ResponseEntity.status(httpStatus).body(response);
    }
}
