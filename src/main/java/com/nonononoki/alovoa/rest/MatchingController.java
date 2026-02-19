package com.nonononoki.alovoa.rest;

import com.nonononoki.alovoa.model.CompatibilityExplanationDto;
import com.nonononoki.alovoa.model.MatchRecommendationDto;
import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.service.MatchingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/matching")
public class MatchingController {

    @Autowired
    private MatchingService matchingService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/daily")
    public ResponseEntity<?> getDailyMatches() {
        try {
            Map<String, Object> result = matchingService.getDailyMatches();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Expo compatibility endpoint.
     * Returns only the matches array, while /daily returns the full payload.
     */
    @GetMapping("/matches")
    public ResponseEntity<?> getMatches() {
        try {
            Map<String, Object> result = matchingService.getDailyMatches();
            Object rawMatches = result.getOrDefault("matches", java.util.Collections.emptyList());
            if (!(rawMatches instanceof List<?> list)) {
                return ResponseEntity.ok(List.of());
            }

            List<Map<String, Object>> mapped = new ArrayList<>();
            for (Object item : list) {
                if (!(item instanceof MatchRecommendationDto dto)) {
                    continue;
                }
                var user = userRepository.findOptionalByUuid(java.util.UUID.fromString(dto.getUserUuid())).orElse(null);
                if (user == null) {
                    continue;
                }

                Map<String, Object> userMap = new HashMap<>();
                userMap.put("uuid", user.getUuid().toString());
                userMap.put("firstName", user.getFirstName());
                userMap.put("age", calculateAge(user));
                userMap.put("verified", user.isVideoVerified());
                userMap.put("profilePicture", user.getProfilePicture() != null
                        ? "/media/profile-picture/" + user.getProfilePicture().getUuid()
                        : "");
                userMap.put("locationName", user.getCountry() != null ? user.getCountry() : "");

                Map<String, Object> compatibility = new HashMap<>();
                compatibility.put("overallScore", dto.getCompatibilityScore() != null ? dto.getCompatibilityScore() : 0.0);
                compatibility.put("enemyScore", dto.getEnemyScore() != null ? dto.getEnemyScore() : 0.0);
                compatibility.put("matchPercentage", dto.getMatchPercentage() != null ? dto.getMatchPercentage() : dto.getCompatibilityScore());

                mapped.add(Map.of(
                        "user", userMap,
                        "compatibilityScore", compatibility,
                        "matchCategory", dto.getMatchCategory() != null ? dto.getMatchCategory() : "Potential Match",
                        "matchInsight", dto.getMatchInsight() != null ? dto.getMatchInsight() : "",
                        "topCompatibilityAreas", dto.getTopCompatibilityAreas() != null ? dto.getTopCompatibilityAreas() : List.of(),
                        "areasToDiscuss", dto.getAreasToDiscuss() != null ? dto.getAreasToDiscuss() : List.of()
                ));
            }
            return ResponseEntity.ok(mapped);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Expo compatibility endpoint for daily limit metadata.
     */
    @GetMapping("/daily-limit")
    public ResponseEntity<?> getDailyLimit() {
        try {
            return ResponseEntity.ok(matchingService.getDailyLimitStatus());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Expo compatibility endpoint.
     * Generates (or refreshes) daily matches and returns the full payload.
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshMatches() {
        try {
            Map<String, Object> result = matchingService.getDailyMatches();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/compatibility/{matchUuid}")
    public ResponseEntity<?> getCompatibilityExplanation(@PathVariable String matchUuid) {
        try {
            CompatibilityExplanationDto result = matchingService.getCompatibilityExplanation(matchUuid);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Mobile compatibility alias.
     */
    @GetMapping("/score/{matchUuid}")
    public ResponseEntity<?> getScore(@PathVariable String matchUuid) {
        return getCompatibilityExplanation(matchUuid);
    }

    /**
     * Expo compatibility endpoint alias for compatibility breakdown.
     */
    @GetMapping("/breakdown/{matchUuid}")
    public ResponseEntity<?> getBreakdown(@PathVariable String matchUuid) {
        try {
            CompatibilityExplanationDto dto = matchingService.getCompatibilityExplanation(matchUuid);

            List<Map<String, Object>> dimensions = new ArrayList<>();
            if (dto.getCategoryBreakdown() != null) {
                dto.getCategoryBreakdown().forEach((k, v) -> {
                    Map<String, Object> dimension = new HashMap<>();
                    dimension.put("dimension", k != null ? k.toLowerCase() : "unknown");
                    dimension.put("score", v != null ? v : 0.0);
                    dimensions.add(dimension);
                });
            } else if (dto.getDimensionScores() != null) {
                dto.getDimensionScores().forEach((k, v) -> {
                    Map<String, Object> dimension = new HashMap<>();
                    dimension.put("dimension", k != null ? k : "unknown");
                    dimension.put("score", v != null ? v : 0.0);
                    dimensions.add(dimension);
                });
            }

            List<Map<String, Object>> dealbreakers = new ArrayList<>();
            if (dto.getDealbreakers() != null) {
                for (CompatibilityExplanationDto.DealbreakderDetail d : dto.getDealbreakers()) {
                    dealbreakers.add(Map.of(
                            "category", d.getCategory() != null ? d.getCategory() : "DEALBREAKER",
                            "description", d.getQuestion() != null ? d.getQuestion() : "Potential conflict"
                    ));
                }
            }

            List<Map<String, Object>> sharedQuestions = new ArrayList<>();
            if (dto.getQuestionMatches() != null) {
                for (CompatibilityExplanationDto.QuestionMatch q : dto.getQuestionMatches()) {
                    sharedQuestions.add(Map.of(
                            "questionText", q.getQuestionText() != null ? q.getQuestionText() : "",
                            "yourAnswer", q.getYourAnswer() != null ? q.getYourAnswer() : "",
                            "theirAnswer", q.getTheirAnswer() != null ? q.getTheirAnswer() : ""
                    ));
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("overallScore", dto.getOverallScore() != null ? dto.getOverallScore() : 0.0);
            result.put("matchCategoryLabel", dto.getMatchCategoryLabel());
            result.put("dimensions", dimensions);
            result.put("dealbreakers", dealbreakers);
            result.put("sharedQuestions", sharedQuestions);
            result.put("summary", dto.getSummary());
            result.put("enemyScore", dto.getEnemyScore() != null ? dto.getEnemyScore() : 0.0);
            result.put("hasDealbreaker", dto.getHasDealbreaker() != null ? dto.getHasDealbreaker() : false);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Mobile compatibility endpoint for filter metadata.
     */
    @GetMapping("/filter")
    public ResponseEntity<?> getFilterMetadata() {
        return ResponseEntity.ok(Map.of(
                "supportsMinMatchPercent", true,
                "supportsDealbreakerFilter", true,
                "supportsEnemyScore", true
        ));
    }

    /**
     * Mobile compatibility endpoint for top N match cards.
     */
    @GetMapping("/top/{limit}")
    public ResponseEntity<?> getTopMatches(@PathVariable int limit) {
        ResponseEntity<?> matchesResponse = getMatches();
        Object body = matchesResponse.getBody();
        if (!(body instanceof List<?> list)) {
            return matchesResponse;
        }
        int safeLimit = Math.max(0, limit);
        if (safeLimit >= list.size()) {
            return ResponseEntity.ok(list);
        }
        return ResponseEntity.ok(new ArrayList<>(list.subList(0, safeLimit)));
    }

    private int calculateAge(com.nonononoki.alovoa.entity.User user) {
        if (user.getDates() == null || user.getDates().getDateOfBirth() == null) {
            return 0;
        }
        LocalDate dob = user.getDates().getDateOfBirth().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        return Period.between(dob, LocalDate.now()).getYears();
    }
}
