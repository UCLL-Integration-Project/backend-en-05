package be.ucll.it.courses.backend.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record CommandResponse(
    @JsonProperty("command_id")
    UUID commandId,
    String status
) {}
