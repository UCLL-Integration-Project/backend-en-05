package be.ucll.it.courses.backend.controller.dto;

import java.util.List;

public record EventListResponse(
    long total,
    List<EventListItemResponse> events
) {}
