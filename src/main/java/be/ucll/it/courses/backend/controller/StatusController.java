package be.ucll.it.courses.backend.controller;

import be.ucll.it.courses.backend.controller.dto.RobotStatusResponse;
import be.ucll.it.courses.backend.exception.ResourceNotFoundException;
import be.ucll.it.courses.backend.service.StatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/v1/status")
public class StatusController {

    private final StatusService statusService;

    @Autowired
    public StatusController(StatusService statusService) {
        this.statusService = statusService;
    }

    @GetMapping
    public ResponseEntity<RobotStatusResponse> getStatus() {
        Optional<RobotStatusResponse> status = statusService.getRobotStatus();
        return status.map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("No robot status found. Has any robot connected yet?"));
    }
}
