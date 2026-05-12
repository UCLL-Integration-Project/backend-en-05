package be.ucll.it.courses.backend.controller.dto;

import java.util.UUID;

public record EventResponse(
    UUID incident_id,
    String status
) {}
