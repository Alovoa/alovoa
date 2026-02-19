package com.nonononoki.alovoa.rest;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.AssessmentResponseDto;
import com.nonononoki.alovoa.model.IntakeProgressDto;
import com.nonononoki.alovoa.model.IntakeStep;
import com.nonononoki.alovoa.service.AuthService;
import com.nonononoki.alovoa.service.IntakeEncouragementService;
import com.nonononoki.alovoa.service.IntakeService;
import com.nonononoki.alovoa.service.ProfileScaffoldingService;
import com.nonononoki.alovoa.service.VideoAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for user intake/onboarding flow.
 * Handles the multi-step process of completing a user's profile
 * including answering core questions, uploading media, and AI analysis.
 */
@RestController
@RequestMapping({"/intake", "/api/v1/intake"})
public class IntakeController {

    @Autowired
    private IntakeService intakeService;

    @Autowired
    private VideoAnalysisService videoAnalysisService;

    @Autowired
    private IntakeEncouragementService encouragementService;

    @Autowired
    private AuthService authService;

    @Autowired
    private ProfileScaffoldingService scaffoldingService;

    /**
     * Get the current user's intake progress with encouraging content.
     * Shows which steps are complete, what's next, and provides warm, inviting messaging.
     */
    @GetMapping("/progress")
    public ResponseEntity<?> getProgress() {
        try {
            IntakeProgressDto progress = intakeService.getIntakeProgress();

            // Add encouraging content
            var user = authService.getCurrentUser(true);
            String currentStep = progress.getNextStep() != null ? progress.getNextStep() : "complete";
            Map<String, Object> encouragement = encouragementService.getIntakeEncouragement(user, currentStep);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("progress", progress);
            response.put("encouragement", encouragement);
            response.put("platformStats", encouragementService.getPlatformStats());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get the 10 core questions (1 from each category) with encouraging context.
     * These are the minimum questions required during intake.
     */
    @GetMapping("/questions")
    public ResponseEntity<?> getCoreQuestions() {
        try {
            List<Map<String, Object>> questions = intakeService.getCoreQuestions();

            // Add category hints to each question
            for (Map<String, Object> q : questions) {
                String category = (String) q.get("category");
                if (category != null) {
                    q.put("categoryHint", encouragementService.getQuestionCategoryHint(category));
                }
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("questions", questions);
            response.put("totalRequired", 10);
            response.put("header", Map.of(
                    "title", "Let's get to know you",
                    "subtitle", "Just 10 questions. No wrong answers. Be yourself.",
                    "encouragement", encouragementService.getStepEncouragement("questions")
            ));
            response.put("funFact", encouragementService.getRelationshipFact());
            response.put("hobbyInsight", encouragementService.getHobbyInsight());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Submit answers to core questions.
     * Users must answer all 10 core questions to complete this step.
     */
    @PostMapping("/questions/submit")
    public ResponseEntity<?> submitAnswers(@RequestBody List<AssessmentResponseDto> responses) {
        try {
            if (responses == null || responses.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No responses provided"));
            }
            Map<String, Object> result = intakeService.submitCoreAnswers(responses);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Upload video introduction.
     * The video can optionally be processed by AI to extract transcript and analysis.
     * Set skipAiAnalysis=true to skip AI and enter profile info manually.
     * This step is REQUIRED for intake completion.
     * REQUIRES: Questions must be completed first.
     */
    @PostMapping("/video")
    public ResponseEntity<?> uploadVideo(
            @RequestParam("file") MultipartFile video,
            @RequestParam(value = "skipAiAnalysis", required = false, defaultValue = "false") boolean skipAiAnalysis) {
        try {
            // Validate that user has completed questions before uploading video
            User user = authService.getCurrentUser(true);
            intakeService.validateCanStartStep(user, IntakeStep.VIDEO);

            Map<String, Object> result = intakeService.uploadVideoIntroduction(video, skipAiAnalysis);
            return ResponseEntity.ok(result);
        } catch (AlovoaException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "blocked", true,
                    "requiredStep", "QUESTIONS"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Video upload failed",
                    "details", e.getMessage()
            ));
        }
    }

    /**
     * Manually enter profile information (worldview, background, life story).
     * Use this if AI analysis was skipped or failed.
     */
    @PostMapping("/profile-info")
    public ResponseEntity<?> submitProfileInfo(@RequestBody Map<String, String> profileInfo) {
        try {
            Map<String, Object> result = intakeService.submitManualProfileInfo(profileInfo);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Skip AI analysis for an already uploaded video.
     * User will need to enter profile info manually.
     */
    @PostMapping("/video/skip-analysis")
    public ResponseEntity<?> skipVideoAnalysis() {
        try {
            Map<String, Object> result = intakeService.skipVideoAnalysis();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get video analysis status.
     * Returns the current status of AI processing for the uploaded video.
     */
    @GetMapping("/video/status/{videoId}")
    public ResponseEntity<?> getVideoStatus(@PathVariable Long videoId) {
        try {
            var status = videoAnalysisService.getAnalysisStatus(videoId);
            if (status == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(Map.of(
                    "videoId", videoId,
                    "status", status.name(),
                    "providerAvailable", videoAnalysisService.isProviderAvailable(),
                    "provider", videoAnalysisService.getProviderName()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Retry video analysis if it failed.
     */
    @PostMapping("/video/retry/{videoId}")
    public ResponseEntity<?> retryVideoAnalysis(@PathVariable Long videoId) {
        try {
            videoAnalysisService.retryAnalysis(videoId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Analysis retry initiated"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Upload audio introduction.
     * This step is OPTIONAL.
     */
    @PostMapping("/audio")
    public ResponseEntity<?> uploadAudio(@RequestParam("file") MultipartFile audio) {
        try {
            Map<String, Object> result = intakeService.uploadAudioIntroduction(audio);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Audio upload failed",
                    "details", e.getMessage()
            ));
        }
    }

    /**
     * Check AI provider status.
     * Useful for frontend to know if AI analysis is available.
     */
    @GetMapping("/ai/status")
    public ResponseEntity<?> getAiStatus() {
        return ResponseEntity.ok(Map.of(
                "available", videoAnalysisService.isProviderAvailable(),
                "provider", videoAnalysisService.getProviderName()
        ));
    }

    /**
     * Get video recording tips and encouragement.
     */
    @GetMapping("/video/tips")
    public ResponseEntity<?> getVideoTips() {
        return ResponseEntity.ok(Map.of(
                "header", Map.of(
                        "title", "Record your video intro",
                        "subtitle", "This is your chance to show who you really are",
                        "encouragement", encouragementService.getStepEncouragement("video")
                ),
                "tips", encouragementService.getVideoTips(),
                "funFact", encouragementService.getPopCultureFact(),
                "reminder", "Remember: authenticity beats perfection. The right person will love the real you."
        ));
    }

    /**
     * Get encouragement for a specific intake step.
     */
    @GetMapping("/encouragement/{step}")
    public ResponseEntity<?> getStepEncouragement(@PathVariable String step) {
        try {
            var user = authService.getCurrentUser(true);
            Map<String, Object> encouragement = encouragementService.getIntakeEncouragement(user, step);
            return ResponseEntity.ok(encouragement);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "stepEncouragement", encouragementService.getStepEncouragement(step),
                    "funFact", encouragementService.getRelationshipFact()
            ));
        }
    }

    /**
     * Expo compatibility endpoint.
     * Returns encouragement for the user's current next step.
     */
    @GetMapping("/encouragement")
    public ResponseEntity<?> getCurrentEncouragement() {
        try {
            User user = authService.getCurrentUser(true);
            IntakeProgressDto progress = intakeService.getIntakeProgress();
            String step = progress.getNextStep() != null ? progress.getNextStep() : "complete";
            return ResponseEntity.ok(encouragementService.getIntakeEncouragement(user, step));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Expo compatibility endpoint.
     * Provides step-level status for the intake flow.
     */
    @GetMapping("/step/{step}")
    public ResponseEntity<?> getStepStatus(@PathVariable String step) {
        try {
            IntakeProgressDto progress = intakeService.getIntakeProgress();
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("step", step.toUpperCase());
            response.put("progress", progress);
            response.put("complete", progress.isIntakeComplete());
            response.put("nextStep", progress.getNextStep());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Expo compatibility endpoint.
     * Step completion is handled by dedicated intake endpoints; this is a no-op success wrapper.
     */
    @PostMapping("/complete/{step}")
    public ResponseEntity<?> completeStep(@PathVariable String step) {
        try {
            IntakeProgressDto progress = intakeService.getIntakeProgress();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "step", step.toUpperCase(),
                    "progress", progress
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get personalized life stats for the user.
     */
    @GetMapping("/life-stats")
    public ResponseEntity<?> getLifeStats() {
        try {
            var user = authService.getCurrentUser(true);
            Map<String, Object> stats = encouragementService.getLifeStats(user);

            if (stats.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "message", "Add your birthday to see personalized stats!",
                        "genericFact", "The average person meets about 80,000 people in their lifetime. One of them could be your person."
                ));
            }

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================================
    // PROFILE SCAFFOLDING ENDPOINTS
    // Fast-track users to matchable in ~10 minutes via video intro
    // ============================================================

    /**
     * Get available video segment prompts for recording.
     * Each prompt guides users on what to talk about in their 2-3 minute videos.
     */
    @GetMapping("/scaffolding/prompts")
    public ResponseEntity<?> getScaffoldingPrompts() {
        try {
            return ResponseEntity.ok(Map.of(
                    "prompts", scaffoldingService.getAvailablePrompts(),
                    "header", Map.of(
                            "title", "Record your intro videos",
                            "subtitle", "Just a few short videos to help us understand you better",
                            "encouragement", "These videos let potential matches get to know the real you!"
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get the user's scaffolding progress.
     * Shows which segments are complete and what's next.
     */
    @GetMapping("/scaffolding/progress")
    public ResponseEntity<?> getScaffoldingProgress() {
        try {
            return ResponseEntity.ok(scaffoldingService.getProgress());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get the scaffolded profile from video analysis.
     * Returns AI-inferred scores with confidence indicators for user review.
     */
    @GetMapping("/scaffolded-profile")
    public ResponseEntity<?> getScaffoldedProfile() {
        try {
            return ResponseEntity.ok(scaffoldingService.getScaffoldedProfile());
        } catch (AlovoaException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "hasProfile", false
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Save user adjustments to the scaffolded profile.
     * Users can adjust any scores before confirming.
     */
    @PostMapping("/scaffolded-profile/adjust")
    public ResponseEntity<?> adjustScaffoldedProfile(@RequestBody Map<String, Object> adjustments) {
        try {
            scaffoldingService.saveAdjustments(adjustments);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Adjustments saved"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Confirm the scaffolded profile.
     * Creates UserAssessmentProfile and enables matching.
     */
    @PostMapping("/scaffolded-profile/confirm")
    public ResponseEntity<?> confirmScaffoldedProfile() {
        try {
            return ResponseEntity.ok(scaffoldingService.confirmScaffoldedProfile());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Re-record video intro.
     * Clears existing video and inference data for a fresh start.
     */
    @PostMapping("/scaffolded-profile/re-record")
    public ResponseEntity<?> reRecordVideoIntro() {
        try {
            return ResponseEntity.ok(scaffoldingService.reRecordVideoIntro());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
