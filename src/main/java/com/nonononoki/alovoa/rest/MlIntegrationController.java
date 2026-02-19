package com.nonononoki.alovoa.rest;

import com.nonononoki.alovoa.service.ml.MlIntegrationStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ml")
public class MlIntegrationController {

    private final MlIntegrationStatusService mlIntegrationStatusService;

    public MlIntegrationController(MlIntegrationStatusService mlIntegrationStatusService) {
        this.mlIntegrationStatusService = mlIntegrationStatusService;
    }

    @GetMapping("/integrations/status")
    public ResponseEntity<?> integrationStatus() {
        try {
            return ResponseEntity.ok(mlIntegrationStatusService.status());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/integrations/qdrant/candidate-enrichment")
    public ResponseEntity<?> candidateEnrichment(@RequestParam("user_id") Long userId,
                                                 @RequestParam(name = "segment_key", required = false) String segmentKey,
                                                 @RequestParam(name = "limit", required = false) Integer limit) {
        if (userId == null || userId <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "user_id query param is required and must be positive integer"));
        }
        try {
            return ResponseEntity.ok(mlIntegrationStatusService.candidateEnrichment(userId, segmentKey, limit));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

