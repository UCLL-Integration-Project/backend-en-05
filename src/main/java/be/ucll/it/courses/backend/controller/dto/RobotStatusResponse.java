package be.ucll.it.courses.backend.controller.dto;

import be.ucll.it.courses.backend.model.RobotMode;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RobotStatusResponse(
    RobotMode mode,
    Short battery_pct,
    Boolean wifi_connected,
    OffsetDateTime last_seen,
    UUID last_event_id,
    Short water_level_pct,
    Boolean water_warning,
    Double last_known_lat,
    Double last_known_lng
) {}
