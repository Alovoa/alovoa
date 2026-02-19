package com.nonononoki.alovoa.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.entity.AssessmentQuestion;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.AssessmentResponseDto;
import com.nonononoki.alovoa.repo.AssessmentQuestionRepository;
import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.service.AssessmentService;
import com.nonononoki.alovoa.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/assessment", "/api/v1/assessment"})
public class AssessmentController {

    @Autowired
    private AssessmentService assessmentService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AssessmentQuestionRepository questionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("/questions/{category}")
    public ResponseEntity<?> getQuestionsByCategory(@PathVariable String category) {
        try {
            Map<String, Object> result = assessmentService.getQuestionsByCategory(category);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid category",
                    "validCategories", List.of("BIG_FIVE", "ATTACHMENT", "DEALBREAKER", "VALUES", "LIFESTYLE", "RED_FLAG")
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/questions")
    public ResponseEntity<?> getQuestions(@RequestParam(defaultValue = "25") Integer limit) {
        try {
            Map<String, Object> result = assessmentService.getNextUnansweredQuestions(null, Math.min(limit, 100));
            return ResponseEntity.ok(result.getOrDefault("questions", List.of()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/questions/random/{count}")
    public ResponseEntity<?> getRandomQuestions(@PathVariable Integer count) {
        try {
            Map<String, Object> result = assessmentService.getNextUnansweredQuestions(null, Math.min(count, 100));
            return ResponseEntity.ok(result.getOrDefault("questions", List.of()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/progress")
    public ResponseEntity<?> getAssessmentProgress() {
        try {
            Map<String, Object> result = assessmentService.getAssessmentProgress();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitResponses(@RequestBody List<AssessmentResponseDto> responses) {
        try {
            Map<String, Object> result = assessmentService.submitResponses(responses);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/answer/bulk")
    public ResponseEntity<?> submitAnswerBulk(@RequestBody List<AssessmentResponseDto> responses) {
        return submitResponses(responses);
    }

    @GetMapping("/results")
    public ResponseEntity<?> getAssessmentResults() {
        try {
            Map<String, Object> result = assessmentService.getAssessmentResults();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Expo compatibility endpoint.
     * Returns a flattened profile shape used by mobile screens.
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getAssessmentProfile() {
        try {
            Map<String, Object> results = assessmentService.getAssessmentResults();
            Map<String, Object> profile = buildMobileProfile(results);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/reset")
    public ResponseEntity<?> resetAssessment(@RequestParam(required = false) String category) {
        try {
            Map<String, Object> result = assessmentService.resetAssessment(category);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid category",
                    "validCategories", List.of("BIG_FIVE", "ATTACHMENT", "DEALBREAKER", "VALUES", "LIFESTYLE", "RED_FLAG")
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/recalculate")
    public ResponseEntity<?> recalculateAssessmentProfile() {
        try {
            Map<String, Object> results = assessmentService.getAssessmentResults();
            return ResponseEntity.ok(buildMobileProfile(results));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/categories")
    public ResponseEntity<?> getCategories() {
        List<String> categories = List.of("BIG_FIVE", "ATTACHMENT", "DEALBREAKER", "VALUES", "LIFESTYLE", "RED_FLAG");
        return ResponseEntity.ok(Map.of("categories", categories));
    }

    @PostMapping("/admin/reload-questions")
    public ResponseEntity<?> reloadQuestions() {
        try {
            User user = authService.getCurrentUser(true);
            if (!user.isAdmin()) {
                return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
            }
            assessmentService.loadQuestionsFromJson();
            return ResponseEntity.ok(Map.of("success", true, "message", "Questions reloaded from JSON"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/match/{userUuid}")
    public ResponseEntity<?> calculateMatch(@PathVariable String userUuid) {
        try {
            User currentUser = authService.getCurrentUser(true);
            User matchUser = userRepository.findOptionalByUuid(UUID.fromString(userUuid))
                    .orElseThrow(() -> new Exception("User not found"));

            Map<String, Object> result = assessmentService.calculateOkCupidMatch(currentUser, matchUser);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/match/{userUuid}/explain")
    public ResponseEntity<?> getMatchExplanation(@PathVariable String userUuid) {
        try {
            User currentUser = authService.getCurrentUser(true);
            User matchUser = userRepository.findOptionalByUuid(UUID.fromString(userUuid))
                    .orElseThrow(() -> new Exception("User not found"));

            Map<String, Object> result = assessmentService.getMatchExplanation(currentUser, matchUser);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============== Question Bank API Endpoints ==============

    /**
     * Get the next unanswered question for the current user.
     * Used for progressive questionnaire flow.
     */
    @GetMapping("/next")
    public ResponseEntity<?> getNextQuestion(@RequestParam(required = false) String category) {
        try {
            Map<String, Object> result = assessmentService.getNextQuestion(category);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid category",
                    "validCategories", List.of("BIG_FIVE", "ATTACHMENT", "DEALBREAKER", "VALUES", "LIFESTYLE", "RED_FLAG")
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get a batch of unanswered questions.
     * Useful for preloading questions in the UI.
     */
    @GetMapping("/batch")
    public ResponseEntity<?> getNextQuestionBatch(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            Map<String, Object> result = assessmentService.getNextUnansweredQuestions(category, Math.min(limit, 50));
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid category",
                    "validCategories", List.of("BIG_FIVE", "ATTACHMENT", "DEALBREAKER", "VALUES", "LIFESTYLE", "RED_FLAG")
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Validate an answer before submitting.
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateAnswer(@RequestBody AssessmentResponseDto response) {
        try {
            Map<String, Object> result = assessmentService.validateAnswer(
                    response.getQuestionId(),
                    response.getNumericResponse(),
                    response.getTextResponse()
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Submit a single answer with validation.
     * Alternative to batch submit for progressive questionnaire.
     */
    @PostMapping("/answer")
    public ResponseEntity<?> submitSingleAnswer(@RequestBody AssessmentResponseDto response) {
        try {
            Map<String, Object> result = assessmentService.submitSingleAnswer(
                    response.getQuestionId(),
                    response.getNumericResponse(),
                    response.getTextResponse(),
                    response.getImportance()
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Mobile payload compatibility endpoint.
     * Accepts the AURA app's answer shape and maps it to AssessmentResponseDto semantics.
     */
    @PostMapping("/answer/mobile")
    public ResponseEntity<?> submitMobileAnswer(@RequestBody Map<String, Object> body) {
        try {
            String questionId = body.get("questionId") != null ? String.valueOf(body.get("questionId")) : null;
            String selectedOptionId = body.get("selectedOptionId") != null ? String.valueOf(body.get("selectedOptionId")) : null;
            String importance = mapImportance(body.get("importance"));
            String textResponse = body.get("textResponse") != null ? String.valueOf(body.get("textResponse")) : null;
            String explanation = body.get("explanation") != null ? String.valueOf(body.get("explanation")) : null;

            if (questionId == null || questionId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "questionId is required"));
            }

            AssessmentQuestion question = questionRepository.findByExternalId(questionId)
                    .orElse(null);
            if (question == null) {
                try {
                    question = questionRepository.findById(Long.parseLong(questionId)).orElse(null);
                } catch (NumberFormatException ignored) {
                    // ignore
                }
            }
            if (question == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "question_not_found"));
            }

            String externalId = question.getExternalId();
            Integer numericResponse = mapOptionToNumeric(question, selectedOptionId);
            String acceptableAnswers = mapAcceptableAnswers(question, body.get("acceptableOptionIds"));

            Map<String, Object> result = assessmentService.submitSingleAnswer(
                    externalId,
                    numericResponse,
                    textResponse,
                    importance,
                    acceptableAnswers,
                    explanation,
                    null
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get question bank statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getQuestionBankStats() {
        try {
            Map<String, Object> stats = new java.util.HashMap<>();

            // Get counts by category
            for (String category : List.of("BIG_FIVE", "ATTACHMENT", "DEALBREAKER", "VALUES", "LIFESTYLE", "RED_FLAG")) {
                try {
                    Map<String, Object> categoryData = assessmentService.getQuestionsByCategory(category);
                    stats.put(category, Map.of(
                            "totalQuestions", categoryData.get("totalQuestions"),
                            "answeredQuestions", categoryData.get("answeredQuestions"),
                            "subcategories", categoryData.get("subcategories")
                    ));
                } catch (Exception ignored) {
                    // Category might not exist
                }
            }

            // Get overall progress
            Map<String, Object> progress = assessmentService.getAssessmentProgress();
            stats.put("overallProgress", progress);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> buildMobileProfile(Map<String, Object> results) throws Exception {
        Map<String, Object> profile = new HashMap<>();
        boolean hasResults = Boolean.TRUE.equals(results.get("hasResults"));

        profile.put("profileComplete", Boolean.TRUE.equals(results.get("profileComplete")));
        profile.put("lastUpdated", results.get("lastUpdated"));

        if (!hasResults) {
            profile.put("questionsAnswered", 0);
            profile.put("openness", 0.0);
            profile.put("conscientiousness", 0.0);
            profile.put("extraversion", 0.0);
            profile.put("agreeableness", 0.0);
            profile.put("neuroticism", 0.0);
            profile.put("attachmentStyle", "SECURE");
            return profile;
        }

        Map<String, Object> progress = assessmentService.getAssessmentProgress();
        int answeredCount = 0;
        for (String category : List.of("BIG_FIVE", "ATTACHMENT", "DEALBREAKER", "VALUES", "LIFESTYLE", "RED_FLAG")) {
            Object categoryObj = progress.get(category);
            if (categoryObj instanceof Map<?, ?> map) {
                Object answered = map.get("answered");
                if (answered instanceof Number n) {
                    answeredCount += n.intValue();
                }
            }
        }
        profile.put("questionsAnswered", answeredCount);

        Object bigFiveObj = results.get("bigFive");
        if (bigFiveObj instanceof Map<?, ?> bigFive) {
            profile.put("openness", getNumber(bigFive.get("openness")));
            profile.put("conscientiousness", getNumber(bigFive.get("conscientiousness")));
            profile.put("extraversion", getNumber(bigFive.get("extraversion")));
            profile.put("agreeableness", getNumber(bigFive.get("agreeableness")));
            profile.put("neuroticism", getNumber(bigFive.get("neuroticism")));
        } else {
            profile.put("openness", 50.0);
            profile.put("conscientiousness", 50.0);
            profile.put("extraversion", 50.0);
            profile.put("agreeableness", 50.0);
            profile.put("neuroticism", 50.0);
        }

        Object attachmentObj = results.get("attachment");
        if (attachmentObj instanceof Map<?, ?> attachment) {
            Object rawStyleValue = attachment.containsKey("style") ? attachment.get("style") : "SECURE";
            String rawStyle = String.valueOf(rawStyleValue);
            profile.put("attachmentStyle", normalizeAttachmentStyle(rawStyle));
            profile.put("attachmentAnxiety", getNumber(attachment.get("anxietyScore")));
            profile.put("attachmentAvoidance", getNumber(attachment.get("avoidanceScore")));
        } else {
            profile.put("attachmentStyle", "SECURE");
            profile.put("attachmentAnxiety", 50.0);
            profile.put("attachmentAvoidance", 50.0);
        }

        Object valuesObj = results.get("values");
        if (valuesObj instanceof Map<?, ?> values) {
            profile.put("progressive", getNumber(values.get("progressive")));
            profile.put("egalitarian", getNumber(values.get("egalitarian")));
        }

        Object lifestyleObj = results.get("lifestyle");
        if (lifestyleObj instanceof Map<?, ?> lifestyle) {
            profile.put("socialOrientation", getNumber(lifestyle.get("social")));
            profile.put("healthFocus", getNumber(lifestyle.get("health")));
            profile.put("workLifeBalance", getNumber(lifestyle.get("workLife")));
            profile.put("financialAmbition", getNumber(lifestyle.get("finance")));
        }

        return profile;
    }

    private double getNumber(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        return 0.0;
    }

    private String normalizeAttachmentStyle(String rawStyle) {
        return switch (rawStyle) {
            case "ANXIOUS_PREOCCUPIED" -> "ANXIOUS";
            case "DISMISSIVE_AVOIDANT" -> "AVOIDANT";
            case "FEARFUL_AVOIDANT" -> "FEARFUL_AVOIDANT";
            default -> "SECURE";
        };
    }

    private String mapImportance(Object importance) {
        if (importance == null) {
            return "somewhat";
        }
        if (importance instanceof Number n) {
            return switch (n.intValue()) {
                case 0 -> "irrelevant";
                case 1 -> "a_little";
                case 3 -> "very";
                case 4 -> "mandatory";
                default -> "somewhat";
            };
        }

        String value = String.valueOf(importance).trim().toLowerCase();
        return switch (value) {
            case "0", "irrelevant" -> "irrelevant";
            case "1", "a_little", "little", "a little important" -> "a_little";
            case "3", "very", "very important" -> "very";
            case "4", "mandatory" -> "mandatory";
            default -> "somewhat";
        };
    }

    private Integer mapOptionToNumeric(AssessmentQuestion question, String selectedOptionId) {
        if (selectedOptionId == null || selectedOptionId.isBlank()) {
            return null;
        }

        List<String> optionIds = extractOptionIds(question);
        int optionCount = optionIds.size();

        try {
            int parsed = Integer.parseInt(selectedOptionId);
            if (optionCount > 0) {
                if (parsed >= 1 && parsed <= optionCount) {
                    return parsed;
                }
                if (parsed >= 0 && parsed < optionCount) {
                    return parsed + 1;
                }
            }
            return parsed;
        } catch (NumberFormatException ignored) {
            // continue with option-based mapping
        }

        int optionIndex = optionIds.indexOf(selectedOptionId);

        if (optionIndex < 0 && selectedOptionId.length() == 1) {
            char c = Character.toLowerCase(selectedOptionId.charAt(0));
            if (c >= 'a' && c <= 'z') {
                optionIndex = c - 'a';
            }
        }
        if (optionIndex < 0) {
            optionIndex = 0;
        }

        AssessmentQuestion.ResponseScale scale = question.getResponseScale();
        if (scale == AssessmentQuestion.ResponseScale.BINARY) {
            return optionIndex > 0 ? 1 : 0;
        }
        if (scale == AssessmentQuestion.ResponseScale.FREE_TEXT) {
            return null;
        }

        if (optionCount > 0) {
            return Math.min(optionCount, optionIndex + 1);
        }

        return Math.max(1, optionIndex + 1);
    }

    private String mapAcceptableAnswers(AssessmentQuestion question, Object rawAcceptable) {
        if (!(rawAcceptable instanceof List<?> list) || list.isEmpty()) {
            return null;
        }
        List<Integer> numeric = new ArrayList<>();
        for (Object option : list) {
            Integer mapped = mapOptionToNumeric(question, String.valueOf(option));
            if (mapped != null) {
                numeric.add(mapped);
            }
        }
        if (numeric.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(numeric.stream().distinct().collect(Collectors.toList()));
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> extractOptionIds(AssessmentQuestion question) {
        if (question.getOptions() == null || question.getOptions().isBlank()) {
            return List.of("1", "2", "3", "4", "5");
        }
        try {
            return objectMapper.readTree(question.getOptions())
                    .findValues("id")
                    .stream()
                    .map(node -> node.asText())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return List.of("1", "2", "3", "4", "5");
        }
    }
}
