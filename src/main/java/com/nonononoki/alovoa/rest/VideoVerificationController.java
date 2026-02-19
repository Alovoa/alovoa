package com.nonononoki.alovoa.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserVideo;
import com.nonononoki.alovoa.repo.UserVideoRepository;
import com.nonononoki.alovoa.service.AuthService;
import com.nonononoki.alovoa.service.VideoVerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping({"/video", "/api/v1/video"})
public class VideoVerificationController {

    @Autowired
    private VideoVerificationService videoVerificationService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserVideoRepository userVideoRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/intro/upload")
    public ResponseEntity<?> uploadIntroVideo(@RequestParam("video") MultipartFile video) {
        try {
            Map<String, Object> result = videoVerificationService.uploadIntroVideo(video);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Mobile compatibility endpoint for older two-step upload flows.
     * Intro uploads are now direct multipart to /intro/upload.
     */
    @PostMapping("/intro/start")
    public ResponseEntity<?> startIntroUpload() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "directUpload", true
        ));
    }

    /**
     * Mobile compatibility endpoint for older two-step upload flows.
     * Intro uploads are finalized in /intro/upload.
     */
    @PostMapping("/intro/confirm")
    public ResponseEntity<?> confirmIntroUpload() {
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/verification/start")
    public ResponseEntity<?> startVerification() {
        try {
            Map<String, Object> result = videoVerificationService.startVerificationSession();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/verification/upload")
    public ResponseEntity<?> uploadVerification(
            @RequestParam("video") MultipartFile video,
            @RequestParam("sessionId") String sessionId,
            @RequestParam(value = "metadata", required = false) String metadata) {
        try {
            Map<String, Object> result = videoVerificationService.submitVerification(video, sessionId, metadata);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Expo compatibility endpoint for old confirm flow.
     * Verification is finalized in /verification/upload, so this returns success.
     */
    @PostMapping("/verification/confirm")
    public ResponseEntity<?> confirmVerificationUpload() {
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/verification/retry")
    public ResponseEntity<?> retryVerification() {
        try {
            Map<String, Object> result = videoVerificationService.startVerificationSession();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "sessionId", result.get("sessionId"),
                    "challenges", result.get("challenges")
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/verification/submit")
    public ResponseEntity<?> submitVerification(
            @RequestParam("video") MultipartFile video,
            @RequestParam("sessionId") String sessionId,
            @RequestParam(value = "metadata", required = false) String metadata) {
        try {
            Map<String, Object> result = videoVerificationService.submitVerification(video, sessionId, metadata);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/verification/status")
    public ResponseEntity<?> getVerificationStatus() {
        try {
            Map<String, Object> result = videoVerificationService.getVerificationStatus();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/intro/status")
    public ResponseEntity<?> getIntroStatus() {
        try {
            User user = authService.getCurrentUser(true);
            UserVideo intro = userVideoRepository.findByUserAndIsIntroTrue(user);
            if (intro == null) {
                return ResponseEntity.ok(Map.of("status", "NONE"));
            }

            String status = intro.getTranscript() != null && !intro.getTranscript().isBlank() ? "READY" : "PROCESSING";
            return ResponseEntity.ok(Map.of(
                    "status", status,
                    "videoId", intro.getId(),
                    "videoUrl", intro.getVideoUrl(),
                    "createdAt", intro.getCreatedAt()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/intro/analysis")
    public ResponseEntity<?> getIntroAnalysis() {
        try {
            User user = authService.getCurrentUser(true);
            UserVideo intro = userVideoRepository.findByUserAndIsIntroTrue(user);
            if (intro == null) {
                return ResponseEntity.ok(Map.of());
            }

            Map<String, Object> result = new HashMap<>();
            result.put("transcription", intro.getTranscript());

            if (intro.getSentimentScores() != null && !intro.getSentimentScores().isBlank()) {
                try {
                    result.put("sentiment", objectMapper.readTree(intro.getSentimentScores()));
                } catch (Exception ignored) {
                    result.put("sentimentRaw", intro.getSentimentScores());
                }
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/intro/delete")
    public ResponseEntity<?> deleteIntro() {
        try {
            User user = authService.getCurrentUser(true);
            UserVideo intro = userVideoRepository.findByUserAndIsIntroTrue(user);
            if (intro != null) {
                userVideoRepository.delete(intro);
            }
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/intro/playback/{id}")
    public ResponseEntity<?> getIntroPlayback(@PathVariable Long id) {
        try {
            User user = authService.getCurrentUser(true);
            UserVideo intro = userVideoRepository.findById(id).orElse(null);
            if (intro == null || !intro.getUser().getId().equals(user.getId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "video_not_found"));
            }
            return ResponseEntity.ok(Map.of(
                    "videoId", intro.getId(),
                    "videoUrl", intro.getVideoUrl()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
