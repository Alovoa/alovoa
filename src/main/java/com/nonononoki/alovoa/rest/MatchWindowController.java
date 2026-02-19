package com.nonononoki.alovoa.rest;

import com.nonononoki.alovoa.entity.MatchWindow;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.service.AuthService;
import com.nonononoki.alovoa.service.MatchWindowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for 24-hour match decision windows.
 */
@RestController
@RequestMapping({"/api/v1/match-windows", "/api/v1/match-window"})
public class MatchWindowController {

    @Autowired
    private MatchWindowService windowService;

    @Autowired
    private AuthService authService;

    /**
     * Get all pending decisions for the current user.
     * These are matches waiting for the user to confirm/decline.
     */
    @GetMapping("/pending")
    public ResponseEntity<List<MatchWindow>> getPendingDecisions() throws AlovoaException {
        return ResponseEntity.ok(windowService.getPendingDecisions());
    }

    /**
     * Get matches where user has confirmed but waiting on the other person.
     */
    @GetMapping("/waiting")
    public ResponseEntity<List<MatchWindow>> getWaitingMatches() throws AlovoaException {
        return ResponseEntity.ok(windowService.getWaitingMatches());
    }

    /**
     * Get confirmed matches ready for conversation.
     */
    @GetMapping("/confirmed")
    public ResponseEntity<List<MatchWindow>> getConfirmedMatches() throws AlovoaException {
        return ResponseEntity.ok(windowService.getConfirmedMatches());
    }

    /**
     * Get count of pending decisions (for notification badge).
     */
    @GetMapping("/pending/count")
    public ResponseEntity<Map<String, Integer>> getPendingCount() throws AlovoaException {
        Map<String, Integer> result = new HashMap<>();
        result.put("count", windowService.getPendingCount());
        return ResponseEntity.ok(result);
    }

    /**
     * Get a specific match window.
     */
    @GetMapping("/{uuid}")
    public ResponseEntity<MatchWindow> getWindow(@PathVariable UUID uuid) {
        return windowService.getWindow(uuid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Confirm interest in a match.
     */
    @PostMapping("/{uuid}/confirm")
    public ResponseEntity<?> confirmInterest(@PathVariable UUID uuid) {
        try {
            MatchWindow window = windowService.confirmInterest(uuid);
            return ResponseEntity.ok(window);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Decline a match.
     */
    @PostMapping("/{uuid}/decline")
    public ResponseEntity<?> declineMatch(@PathVariable UUID uuid) {
        try {
            MatchWindow window = windowService.declineMatch(uuid);
            return ResponseEntity.ok(window);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Request a 12-hour extension on the decision window.
     */
    @PostMapping("/{uuid}/extend")
    public ResponseEntity<?> requestExtension(@PathVariable UUID uuid) {
        try {
            MatchWindow window = windowService.requestExtension(uuid);
            return ResponseEntity.ok(window);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get dashboard summary for matches page.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() throws AlovoaException {
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("pending", windowService.getPendingDecisions());
        dashboard.put("waiting", windowService.getWaitingMatches());
        dashboard.put("confirmed", windowService.getConfirmedMatches());
        dashboard.put("pendingCount", windowService.getPendingCount());
        return ResponseEntity.ok(dashboard);
    }

    /**
     * Send an intro message within the match window (Marriage Machine feature).
     * This is the "personality leads" feature - send ONE message before matching.
     * Like OKCupid's original open messaging, but limited to match window.
     */
    @PostMapping("/{uuid}/intro-message")
    public ResponseEntity<?> sendIntroMessage(
            @PathVariable UUID uuid,
            @RequestBody Map<String, String> payload) {
        try {
            String message = payload.get("message");
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Message cannot be empty"));
            }
            if (message.length() > 500) {
                return ResponseEntity.badRequest().body(Map.of("error", "Message too long (max 500 characters)"));
            }
            MatchWindow window = windowService.sendIntroMessage(uuid, message.trim());
            return ResponseEntity.ok(Map.of("success", true, "window", window));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================================
    // Expo Compatibility Endpoints
    // ============================================================

    @GetMapping("/list")
    public ResponseEntity<?> listForExpo() {
        try {
            User currentUser = authService.getCurrentUser(true);
            List<MatchWindow> all = new ArrayList<>();
            all.addAll(windowService.getPendingDecisions());
            all.addAll(windowService.getWaitingMatches());
            all.addAll(windowService.getConfirmedMatches());

            List<Map<String, Object>> mapped = all.stream()
                    .map(w -> mapWindowForExpo(w, currentUser))
                    .toList();
            return ResponseEntity.ok(mapped);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/current")
    public ResponseEntity<?> currentForExpo() {
        try {
            User currentUser = authService.getCurrentUser(true);
            List<MatchWindow> pending = windowService.getPendingDecisions();
            if (pending.isEmpty()) {
                return ResponseEntity.ok().build();
            }
            return ResponseEntity.ok(mapWindowForExpo(pending.get(0), currentUser));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/respond/{uuid}")
    public ResponseEntity<?> respondForExpo(@PathVariable UUID uuid, @RequestBody Map<String, String> body) {
        try {
            String response = body.getOrDefault("response", "").toUpperCase();
            if ("ACCEPT".equals(response)) {
                return ResponseEntity.ok(windowService.confirmInterest(uuid));
            }
            return ResponseEntity.ok(windowService.declineMatch(uuid));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/skip/{uuid}")
    public ResponseEntity<?> skipForExpo(@PathVariable UUID uuid) {
        try {
            MatchWindow window = windowService.requestExtension(uuid);
            return ResponseEntity.ok(Map.of("success", true, "window", window));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> mapWindowForExpo(MatchWindow window, User currentUser) {
        User matchedUser = window.getOtherUser(currentUser);
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", window.getUuid().toString());
        dto.put("status", mapStatus(window.getStatus()));
        dto.put("expiresAt", window.getExpiresAt());
        dto.put("compatibilityScore", window.getCompatibilityScore());
        dto.put("matchCategory", matchCategory(window.getCompatibilityScore()));
        dto.put("matchReason", "You two show strong compatibility across your answers.");
        Map<String, Object> matchedUserDto = new HashMap<>();
        matchedUserDto.put("uuid", matchedUser.getUuid().toString());
        matchedUserDto.put("firstName", matchedUser.getFirstName() != null ? matchedUser.getFirstName() : "");
        matchedUserDto.put("age", calculateAge(matchedUser));
        matchedUserDto.put("verified", matchedUser.isVideoVerified());
        matchedUserDto.put("profilePicture", matchedUser.getProfilePicture() != null
                ? "/media/profile-picture/" + matchedUser.getProfilePicture().getUuid()
                : "");
        matchedUserDto.put("locationName", matchedUser.getCountry() != null ? matchedUser.getCountry() : "");
        dto.put("matchedUser", matchedUserDto);
        return dto;
    }

    private String mapStatus(MatchWindow.WindowStatus status) {
        return switch (status) {
            case PENDING_BOTH, PENDING_USER_A, PENDING_USER_B, EXTENSION_PENDING -> "PENDING";
            case CONFIRMED -> "ACCEPTED";
            case DECLINED_BY_A, DECLINED_BY_B -> "DECLINED";
            case EXPIRED -> "EXPIRED";
        };
    }

    private String matchCategory(Double score) {
        if (score == null) return "Potential Match";
        if (score >= 85) return "Strong Match";
        if (score >= 70) return "Good Match";
        if (score >= 55) return "Fair Match";
        return "Possible Match";
    }

    private int calculateAge(User user) {
        if (user.getDates() == null || user.getDates().getDateOfBirth() == null) {
            return 0;
        }
        java.time.LocalDate dob = user.getDates().getDateOfBirth().toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate();
        return java.time.Period.between(dob, java.time.LocalDate.now()).getYears();
    }
}
