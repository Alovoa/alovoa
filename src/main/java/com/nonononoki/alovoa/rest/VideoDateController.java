package com.nonononoki.alovoa.rest;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.service.AuthService;
import com.nonononoki.alovoa.service.VideoDateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/video-date")
public class VideoDateController {

    @Autowired
    private VideoDateService videoDateService;

    @Autowired
    private AuthService authService;

    @PostMapping("/propose")
    public ResponseEntity<?> proposeVideoDate(
            @RequestParam Long conversationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date proposedTime) {
        try {
            Map<String, Object> result = videoDateService.proposeVideoDate(conversationId, proposedTime);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/respond")
    public ResponseEntity<?> respondToProposal(
            @PathVariable Long id,
            @RequestParam boolean accept,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date counterTime) {
        try {
            Map<String, Object> result = videoDateService.respondToProposal(id, accept, counterTime);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<?> startVideoDate(@PathVariable Long id) {
        try {
            Map<String, Object> result = videoDateService.startVideoDate(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/end")
    public ResponseEntity<?> endVideoDate(@PathVariable Long id) {
        try {
            Map<String, Object> result = videoDateService.endVideoDate(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/feedback")
    public ResponseEntity<?> submitFeedback(
            @PathVariable Long id,
            @RequestBody Map<String, Object> feedback) {
        try {
            Map<String, Object> result = videoDateService.submitFeedback(id, feedback);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/upcoming")
    public ResponseEntity<?> getUpcomingDates() {
        try {
            Map<String, Object> result = videoDateService.getUpcomingDates();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/proposals")
    public ResponseEntity<?> getPendingProposals() {
        try {
            Map<String, Object> result = videoDateService.getPendingProposals();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getDateHistory() {
        try {
            Map<String, Object> result = videoDateService.getDateHistory();
            return ResponseEntity.ok(result);
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
            List<Map<String, Object>> merged = new ArrayList<>();

            Object proposals = videoDateService.getPendingProposals().get("proposals");
            if (proposals instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        merged.add(mapVideoDateForExpo((Map<String, Object>) map, currentUser));
                    }
                }
            }

            Object upcoming = videoDateService.getUpcomingDates().get("dates");
            if (upcoming instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        merged.add(mapVideoDateForExpo((Map<String, Object>) map, currentUser));
                    }
                }
            }

            Object history = videoDateService.getDateHistory().get("history");
            if (history instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        merged.add(mapVideoDateForExpo((Map<String, Object>) map, currentUser));
                    }
                }
            }

            merged = merged.stream()
                    .sorted(Comparator.comparing(
                            m -> parseDate((String) nullSafeToString(m.get("scheduledAt"))),
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();

            return ResponseEntity.ok(merged);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/accept/{id}")
    public ResponseEntity<?> acceptForExpo(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(videoDateService.respondToProposal(id, true, null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/decline/{id}")
    public ResponseEntity<?> declineForExpo(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(videoDateService.respondToProposal(id, false, null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/cancel/{id}")
    public ResponseEntity<?> cancelForExpo(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(videoDateService.cancelDate(String.valueOf(id), null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/schedule/{id}")
    public ResponseEntity<?> scheduleForExpo(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            Object scheduledAtRaw = body.get("scheduledAt");
            Date scheduledAt = parseDate(scheduledAtRaw != null ? String.valueOf(scheduledAtRaw) : null);
            return ResponseEntity.ok(videoDateService.respondToProposal(id, true, scheduledAt));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/join/{id}")
    public ResponseEntity<?> joinForExpo(@PathVariable Long id) {
        return startVideoDate(id);
    }

    @PostMapping("/leave/{id}")
    public ResponseEntity<?> leaveForExpo(@PathVariable Long id) {
        return endVideoDate(id);
    }

    @PostMapping("/feedback/{id}")
    public ResponseEntity<?> feedbackForExpo(@PathVariable Long id, @RequestBody Map<String, Object> feedback) {
        return submitFeedback(id, feedback);
    }

    private Map<String, Object> mapVideoDateForExpo(Map<String, Object> source, User currentUser) {
        Map<String, Object> proposedBy = asObjectMap(source.get("proposedBy"));
        Map<String, Object> proposedTo = asObjectMap(source.get("proposedTo"));

        boolean isInitiator = String.valueOf(proposedBy.getOrDefault("id", "")).equals(String.valueOf(currentUser.getId()));
        Map<String, Object> partner = isInitiator ? proposedTo : proposedBy;

        String status = nullSafeToString(source.get("status"));
        if ("CANCELLED".equals(status)) {
            status = "DECLINED";
        }

        Integer durationMinutes = null;
        Object durationRaw = source.get("duration");
        if (durationRaw instanceof Number n) {
            durationMinutes = Math.max(1, n.intValue() / 60);
        }
        if (durationMinutes == null) {
            durationMinutes = 30;
        }

        Map<String, Object> dto = new HashMap<>();
        dto.put("id", source.get("id"));
        dto.put("status", status != null ? status : "PROPOSED");
        dto.put("scheduledAt", source.get("scheduledAt"));
        dto.put("createdAt", source.get("createdAt"));
        dto.put("partnerId", partner.getOrDefault("id", 0));
        dto.put("partnerName", partner.getOrDefault("name", partner.getOrDefault("firstName", "Match")));
        dto.put("partnerProfilePicture", "");
        dto.put("isInitiator", isInitiator);
        dto.put("durationMinutes", durationMinutes);
        return dto;
    }

    private String nullSafeToString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Date parseDate(String isoDate) {
        if (isoDate == null || isoDate.isBlank()) {
            return null;
        }
        try {
            return Date.from(Instant.parse(isoDate));
        } catch (Exception ignored) {
            try {
                return Date.from(java.time.OffsetDateTime.parse(isoDate).toInstant());
            } catch (Exception ignoredAgain) {
                try {
                    return Date.from(java.time.LocalDateTime.parse(isoDate)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toInstant());
                } catch (Exception e) {
                    return null;
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asObjectMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return new HashMap<>();
    }
}
