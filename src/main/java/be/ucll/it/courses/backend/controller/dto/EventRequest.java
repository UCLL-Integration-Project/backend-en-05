package be.ucll.it.courses.backend.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.OffsetDateTime;

public record EventRequest(
    @NotNull(message = "Timestamp is required")
    OffsetDateTime timestamp,

    @NotNull(message = "Temperature is required")
    Float temperature,

    @NotNull(message = "Battery percentage is required")
    @Min(value = 0, message = "Battery percentage must be at least 0")
    @Max(value = 100, message = "Battery percentage must be at most 100")
    Short battery_pct,

    @Min(value = 0, message = "Water level percentage must be at least 0")
    @Max(value = 100, message = "Water level percentage must be at most 100")
    Short water_level_pct,

    Integer duration_s,

    Boolean is_extinguished,
    Double latitude,
    Double longitude,
    Integer locationAccuracy_m,

    @Pattern(regexp = "fire|low_water", message = "event_type must be 'fire' or 'low_water'")
    String event_type
) {}
