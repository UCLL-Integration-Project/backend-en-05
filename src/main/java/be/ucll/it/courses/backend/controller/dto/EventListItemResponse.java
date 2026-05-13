package be.ucll.it.courses.backend.controller.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventListItemResponse(
    UUID incident_id,
    OffsetDateTime timestamp,
    Float temperature,
    Short battery_pct,
    Integer duration_s,
    Boolean is_extinguished
) {}
