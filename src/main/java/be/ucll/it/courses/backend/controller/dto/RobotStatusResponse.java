package be.ucll.it.courses.backend.controller.dto;

import be.ucll.it.courses.backend.model.RobotMode;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RobotStatusResponse(
    RobotMode mode,
    Integer battery_pct,
    Boolean wifi_connected,
    OffsetDateTime last_seen,
    UUID last_event_id
) {}
