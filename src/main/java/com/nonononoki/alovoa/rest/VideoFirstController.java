package com.nonononoki.alovoa.rest;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.service.AuthService;
import com.nonononoki.alovoa.service.VideoFirstService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for video-first display functionality.
 * Handles recording video watches and checking watch status.
 */
@RestController
@RequestMapping("/api/video-first")
public class VideoFirstController {

    @Autowired
    private VideoFirstService videoFirstService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepo;

    /**
     * Record that the current user started watching a video
     */
    @PostMapping("/watch/{profileUuid}")
    public ResponseEntity<Map<String, Object>> recordWatch(
            @PathVariable UUID profileUuid) throws AlovoaException {

        User viewer = authService.getCurrentUser(true);
        User profileOwner = userRepo.findByUuid(profileUuid);
        if (profileOwner == null) {
            throw new AlovoaException("User not found");
        }

        videoFirstService.recordVideoWatch(viewer, profileOwner);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("videoWatched", true);
        return ResponseEntity.ok(response);
    }

    /**
     * Update video watch progress
     */
    @PostMapping("/watch/{profileUuid}/progress")
    public ResponseEntity<Map<String, Object>> updateProgress(
            @PathVariable UUID profileUuid,
            @RequestBody WatchProgressRequest request) throws AlovoaException {

        User viewer = authService.getCurrentUser(true);
        User profileOwner = userRepo.findByUuid(profileUuid);
        if (profileOwner == null) {
            throw new AlovoaException("User not found");
        }

        videoFirstService.updateWatchProgress(viewer, profileOwner,
                request.durationSeconds, request.completed);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("completed", request.completed);
        return ResponseEntity.ok(response);
    }

    /**
     * Check if current user has watched a profile's video
     */
    @GetMapping("/status/{profileUuid}")
    public ResponseEntity<Map<String, Object>> checkWatchStatus(
            @PathVariable UUID profileUuid) throws AlovoaException {

        User viewer = authService.getCurrentUser(true);
        User profileOwner = userRepo.findByUuid(profileUuid);
        if (profileOwner == null) {
            throw new AlovoaException("User not found");
        }

        boolean hasWatched = videoFirstService.hasWatchedVideo(viewer, profileOwner);
        boolean hasCompleted = videoFirstService.hasCompletedWatching(viewer, profileOwner);
        boolean shouldBlur = videoFirstService.shouldBlurPhotos(viewer, profileOwner);

        Map<String, Object> response = new HashMap<>();
        response.put("hasWatched", hasWatched);
        response.put("hasCompleted", hasCompleted);
        response.put("shouldBlurPhotos", shouldBlur);
        response.put("hasVideoIntro", profileOwner.getVideoIntroduction() != null);
        response.put("videoWatchRequired", profileOwner.isRequireVideoFirst());
        return ResponseEntity.ok(response);
    }

    /**
     * Toggle video-first requirement for current user's profile
     */
    @PostMapping("/require-video-first")
    public ResponseEntity<Map<String, Object>> setRequireVideoFirst(
            @RequestBody RequireVideoFirstRequest request) throws AlovoaException {

        User user = authService.getCurrentUser(true);

        // Only allow enabling if user has a video intro
        if (request.enabled && user.getVideoIntroduction() == null) {
            throw new AlovoaException("Cannot enable video-first without a video introduction");
        }

        user.setRequireVideoFirst(request.enabled);
        userRepo.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("requireVideoFirst", request.enabled);
        return ResponseEntity.ok(response);
    }

    /**
     * Get watch statistics for current user's video
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getWatchStats() throws AlovoaException {
        User user = authService.getCurrentUser(true);

        VideoFirstService.WatchStats stats = videoFirstService.getWatchStats(user);

        Map<String, Object> response = new HashMap<>();
        response.put("totalWatches", stats.totalWatches);
        response.put("completedWatches", stats.completedWatches);
        response.put("completionRate", stats.completionRate);
        return ResponseEntity.ok(response);
    }

    // Request DTOs

    public static class WatchProgressRequest {
        public int durationSeconds;
        public boolean completed;
    }

    public static class RequireVideoFirstRequest {
        public boolean enabled;
    }
}
