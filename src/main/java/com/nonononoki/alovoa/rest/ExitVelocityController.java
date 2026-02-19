package com.nonononoki.alovoa.rest;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.ExitVelocityEvent;
import com.nonononoki.alovoa.entity.user.ExitVelocityMetrics;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.service.AuthService;
import com.nonononoki.alovoa.service.ExitVelocityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for Exit Velocity tracking - the primary success metric.
 */
@RestController
@RequestMapping("/api/exit-velocity")
public class ExitVelocityController {

    @Autowired
    private ExitVelocityService exitVelocityService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepo;

    /**
     * Record that a relationship was formed
     */
    @PostMapping("/relationship-formed")
    public ResponseEntity<Map<String, Object>> recordRelationshipFormed(
            @RequestBody RelationshipFormedRequest request) throws AlovoaException {

        User user = authService.getCurrentUser(true);
        User partner = null;

        if (request.partnerUuid != null) {
            partner = userRepo.findByUuid(request.partnerUuid);
        }

        ExitVelocityEvent event = exitVelocityService.recordRelationshipFormed(user, partner);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("daysToRelationship", event.getDaysToRelationship());
        response.put("message", "Congratulations! We're so happy you found someone.");
        return ResponseEntity.ok(response);
    }

    /**
     * Submit exit survey
     */
    @PostMapping("/exit-survey")
    public ResponseEntity<Map<String, Object>> submitExitSurvey(
            @RequestBody ExitSurveyRequest request) throws AlovoaException {

        User user = authService.getCurrentUser(true);

        ExitVelocityService.ExitSurveyResponse survey = new ExitVelocityService.ExitSurveyResponse();
        survey.exitType = request.exitType;
        survey.reason = request.reason;
        survey.satisfactionRating = request.satisfactionRating;
        survey.feedback = request.feedback;
        survey.wouldRecommend = request.wouldRecommend;

        exitVelocityService.recordExitSurvey(user, survey);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Thank you for your feedback. We wish you all the best!");
        return ResponseEntity.ok(response);
    }

    /**
     * Get exit velocity summary (admin/public dashboard)
     */
    @GetMapping("/summary")
    public ResponseEntity<ExitVelocityService.ExitVelocitySummary> getSummary() throws AlovoaException {
        authService.getCurrentUser(true); // Ensure authenticated

        ExitVelocityService.ExitVelocitySummary summary = exitVelocityService.getSummary();
        return ResponseEntity.ok(summary);
    }

    /**
     * Get metrics history for charting
     */
    @GetMapping("/metrics/history")
    public ResponseEntity<List<MetricsDto>> getMetricsHistory(
            @RequestParam(defaultValue = "30") int days) throws AlovoaException {

        // This could be admin-only or public depending on policy
        authService.getCurrentUser(true);

        List<ExitVelocityMetrics> metrics = exitVelocityService.getMetricsHistory(days);

        List<MetricsDto> dtos = metrics.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * Get platform success stats (public)
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getPublicStats() throws AlovoaException {
        ExitVelocityService.ExitVelocitySummary summary = exitVelocityService.getSummary();

        Map<String, Object> stats = new HashMap<>();
        stats.put("relationshipsThisMonth", summary.relationshipsThisMonth);
        stats.put("avgDaysToRelationship", summary.avgDaysToRelationship);
        stats.put("recommendationRate", summary.recommendationRate);

        // Create user-friendly messages
        if (summary.avgDaysToRelationship != null && summary.avgDaysToRelationship > 0) {
            stats.put("velocityMessage", String.format("On average, our users find meaningful connections in %.0f days",
                    summary.avgDaysToRelationship));
        }

        if (summary.recommendationRate != null && summary.recommendationRate > 0) {
            stats.put("recommendationMessage", String.format("%.0f%% of users would recommend us to friends",
                    summary.recommendationRate * 100));
        }

        return ResponseEntity.ok(stats);
    }

    // Helper methods

    private MetricsDto toDto(ExitVelocityMetrics m) {
        MetricsDto dto = new MetricsDto();
        dto.date = m.getMetricDate().toString();
        dto.totalExits = m.getTotalExits();
        dto.positiveExits = m.getPositiveExits();
        dto.relationshipsFormed = m.getRelationshipsFormed();
        dto.avgDaysToRelationship = m.getAvgDaysToRelationship();
        dto.avgSatisfaction = m.getAvgSatisfaction();
        dto.recommendationRate = m.getRecommendationRate();
        dto.activeUsers = m.getActiveUsers();
        dto.positiveExitRate = m.getPositiveExitRate();
        return dto;
    }

    // DTOs

    public static class RelationshipFormedRequest {
        public UUID partnerUuid;
    }

    public static class ExitSurveyRequest {
        public ExitVelocityEvent.ExitEventType exitType;
        public String reason;
        public Integer satisfactionRating;
        public String feedback;
        public Boolean wouldRecommend;
    }

    public static class MetricsDto {
        public String date;
        public int totalExits;
        public int positiveExits;
        public int relationshipsFormed;
        public Double avgDaysToRelationship;
        public Double avgSatisfaction;
        public Double recommendationRate;
        public int activeUsers;
        public Double positiveExitRate;
    }
}
