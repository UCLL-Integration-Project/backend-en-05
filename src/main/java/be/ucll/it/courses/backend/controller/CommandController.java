package be.ucll.it.courses.backend.controller;

import be.ucll.it.courses.backend.controller.dto.CommandRequest;
import be.ucll.it.courses.backend.controller.dto.CommandResponse;
import be.ucll.it.courses.backend.model.CommandAction;
import be.ucll.it.courses.backend.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1")
public class CommandController {

    private final UserRepository userRepository;

    public CommandController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/command")
    public ResponseEntity<?> sendCommand(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Role", required = false) String roleHeader,
            @Valid @RequestBody CommandRequest request,
            jakarta.servlet.http.HttpServletRequest servletRequest) {

        String token = AuthController.extractToken(servletRequest, authHeader);
        if (token == null || userRepository.findByToken(token).isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 403 Forbidden
        if (roleHeader == null || !roleHeader.equalsIgnoreCase("admin")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // 422 Unprocessable Entity for invalid actions
        CommandAction action;
        try {
            action = CommandAction.valueOf(request.action());
        } catch (IllegalArgumentException | NullPointerException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }

        // 202 Accepted
        UUID commandId = UUID.randomUUID();
        // duration_ms (defaulting to 500) is implicitly handled by the response logic
        // in a real scenario we'd use 'duration' to dispatch the command

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new CommandResponse(commandId, "dispatched"));
    }
}
