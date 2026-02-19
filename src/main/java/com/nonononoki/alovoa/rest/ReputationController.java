package com.nonononoki.alovoa.rest;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserBehaviorEvent;
import com.nonononoki.alovoa.entity.user.UserReputationScore;
import com.nonononoki.alovoa.service.AuthService;
import com.nonononoki.alovoa.service.ReputationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/reputation")
public class ReputationController {

    @Autowired
    private ReputationService reputationService;

    @Autowired
    private AuthService authService;

    @GetMapping("/me")
    public ResponseEntity<?> getMyReputation() {
        try {
            User user = authService.getCurrentUser(true);
            UserReputationScore reputation = reputationService.getOrCreateReputation(user);

            Map<String, Object> result = new HashMap<>();
            // Use categorical labels instead of numerical scores to prevent gaming behavior
            result.put("standingLabel", getStandingLabel(reputation.getOverallScore()));
            result.put("standingLevel", formatTrustLevel(reputation.getTrustLevel()));
            result.put("factors", Map.of(
                    "responseQuality", scoreToFactorLabel(reputation.getResponseQuality()),
                    "respect", scoreToFactorLabel(reputation.getRespectScore()),
                    "authenticity", scoreToFactorLabel(reputation.getAuthenticityScore()),
                    "investment", scoreToFactorLabel(reputation.getInvestmentScore())
            ));
            result.put("trustLevel", reputation.getTrustLevel().name());
            result.put("stats", Map.of(
                    "datesCompleted", reputation.getDatesCompleted(),
                    "positiveFeedback", reputation.getPositiveFeedbackCount()
            ));

            // Include appeal status if applicable
            if (reputation.getProbationUntil() != null) {
                result.put("onProbation", true);
                result.put("probationEndsAt", reputation.getProbationUntil());
            }
            if (Boolean.TRUE.equals(reputation.getAppealPending())) {
                result.put("appealPending", true);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Expo compatibility endpoint.
     * Returns a numeric score breakdown used by legacy mobile UI.
     */
    @GetMapping("/score")
    public ResponseEntity<?> getMyReputationScore() {
        try {
            User user = authService.getCurrentUser(true);
            UserReputationScore reputation = reputationService.getOrCreateReputation(user);

            int verificationPoints = user.isVideoVerified() ? 200 : 0;
            int responsePoints = (int) Math.round(safeScore(reputation.getResponseQuality()) * 4);
            int reliabilityPoints = (int) Math.round(safeScore(reputation.getRespectScore()) * 4);
            int feedbackPoints = Math.min(400, Math.max(0, reputation.getPositiveFeedbackCount() * 20));
            int tenurePoints = (int) Math.round(safeScore(reputation.getInvestmentScore()) * 4);
            int authenticityPoints = (int) Math.round(safeScore(reputation.getAuthenticityScore()) * 4);

            int totalScore = verificationPoints + responsePoints + reliabilityPoints + feedbackPoints + tenurePoints + authenticityPoints;

            return ResponseEntity.ok(Map.of(
                    "totalScore", totalScore,
                    "verificationPoints", verificationPoints,
                    "responsePoints", responsePoints,
                    "reliabilityPoints", reliabilityPoints,
                    "feedbackPoints", feedbackPoints,
                    "tenurePoints", tenurePoints,
                    "trustLevel", reputation.getTrustLevel().name(),
                    "updatedAt", reputation.getUpdatedAt()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private String getStandingLabel(Double score) {
        if (score == null) return "Building";
        if (score >= 80) return "Excellent Standing";
        if (score >= 65) return "Good Standing";
        if (score >= 50) return "Fair Standing";
        if (score >= 30) return "Building";
        return "Needs Attention";
    }

    private String scoreToFactorLabel(Double score) {
        if (score == null) return "New";
        if (score >= 80) return "Excellent";
        if (score >= 60) return "Good";
        if (score >= 40) return "Fair";
        if (score >= 20) return "Building";
        return "New";
    }

    private double safeScore(Double value) {
        return value != null ? value : 0.0;
    }

    @GetMapping("/badges")
    public ResponseEntity<?> getMyBadges() {
        try {
            User user = authService.getCurrentUser(true);
            UserReputationScore reputation = reputationService.getOrCreateReputation(user);

            List<Map<String, Object>> badges = new java.util.ArrayList<>();

            // Trust level badge
            badges.add(Map.of(
                    "id", "trust_" + reputation.getTrustLevel().name().toLowerCase(),
                    "name", formatTrustLevel(reputation.getTrustLevel()),
                    "type", "trust"
            ));

            // Video verified badge
            if (user.isVideoVerified()) {
                badges.add(Map.of(
                        "id", "video_verified",
                        "name", "Video Verified",
                        "type", "verification"
                ));
            }

            // Response quality badge
            if (safeScore(reputation.getResponseQuality()) >= 80) {
                badges.add(Map.of(
                        "id", "responsive",
                        "name", "Great Communicator",
                        "type", "quality"
                ));
            }

            // Dates completed badge
            if (reputation.getDatesCompleted() != null && reputation.getDatesCompleted() >= 5) {
                badges.add(Map.of(
                        "id", "active_dater",
                        "name", "Active Dater",
                        "type", "activity"
                ));
            }

            return ResponseEntity.ok(Map.of("badges", badges));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getReputationHistory(@RequestParam(defaultValue = "30") Integer days) {
        try {
            User user = authService.getCurrentUser(true);
            List<UserBehaviorEvent> events = reputationService.getRecentBehavior(user, days);

            List<Map<String, Object>> history = events.stream()
                    .map(e -> Map.<String, Object>of(
                            "type", e.getBehaviorType().name(),
                            "impact", e.getReputationImpact(),
                            "date", e.getCreatedAt()
                    ))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("history", history));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private String formatTrustLevel(UserReputationScore.TrustLevel level) {
        switch (level) {
            case NEW_MEMBER: return "New Member";
            case VERIFIED: return "Verified";
            case TRUSTED: return "Trusted";
            case HIGHLY_TRUSTED: return "Highly Trusted";
            case UNDER_REVIEW: return "Under Review";
            case PROBATION: return "On Probation";
            case RESTRICTED: return "Restricted";
            default: return level.name();
        }
    }
}
