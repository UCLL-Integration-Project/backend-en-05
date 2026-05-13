package be.ucll.it.courses.backend.controller.dto;

public record HealthResponse(
    String status,
    String version,
    long uptime_s,
    String db
) {}
