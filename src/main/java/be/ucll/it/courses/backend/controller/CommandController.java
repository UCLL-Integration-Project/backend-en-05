package be.ucll.it.courses.backend.controller;

import be.ucll.it.courses.backend.controller.dto.CommandRequest;
import be.ucll.it.courses.backend.controller.dto.CommandResponse;
import be.ucll.it.courses.backend.model.CommandAction;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1")
public class CommandController {

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/command")
    public ResponseEntity<?> sendCommand(@Valid @RequestBody CommandRequest request) {
        CommandAction action;
        try {
            action = CommandAction.valueOf(request.action());
        } catch (IllegalArgumentException | NullPointerException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new CommandResponse(UUID.randomUUID(), "dispatched"));
    }
}
