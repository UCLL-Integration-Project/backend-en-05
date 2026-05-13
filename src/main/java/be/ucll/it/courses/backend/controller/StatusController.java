package be.ucll.it.courses.backend.controller;

import be.ucll.it.courses.backend.controller.dto.RobotStatusResponse;
import be.ucll.it.courses.backend.service.StatusService;
import be.ucll.it.courses.backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/v1/status")
public class StatusController {

    private final StatusService statusService;
    private final UserRepository userRepository;

    @Autowired
    public StatusController(StatusService statusService, UserRepository userRepository) {
        this.statusService = statusService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<RobotStatusResponse> getStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            HttpServletRequest request) {

        // Accept session cookie or Bearer token
        String token = AuthController.extractTokenFromCookie(request);
        if (token == null && authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
        }

        if (token == null || userRepository.findByToken(token).isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<RobotStatusResponse> status = statusService.getRobotStatus();
        
        return status.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
