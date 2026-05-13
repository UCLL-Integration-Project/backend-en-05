package be.ucll.it.courses.backend.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record CommandRequest(
    @NotNull(message = "Action is mandatory")
    String action,
    
    @JsonProperty("duration_ms")
    Integer durationMs
) {}
