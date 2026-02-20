package com.nonononoki.alovoa.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Lightweight health endpoint compatible with expected actuator path.
 * This keeps local no-docker UX validation unblocked even when actuator is not present.
 */
@RestController
public class HealthController {

    @GetMapping("/actuator/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", Instant.now().toString()
        ));
    }
}
