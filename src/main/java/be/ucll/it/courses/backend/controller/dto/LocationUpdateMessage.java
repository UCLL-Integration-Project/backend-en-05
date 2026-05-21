package be.ucll.it.courses.backend.controller.dto;

public record LocationUpdateMessage(
        String type,
        double latitude,
        double longitude,
        double accuracy_m,
        String timestamp
) {}
