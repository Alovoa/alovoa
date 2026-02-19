package com.nonononoki.alovoa.rest.admin;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.matching.rerank.service.MatchingAnalyticsService;
import com.nonononoki.alovoa.matching.rerank.service.RollingStatsAggregationService;
import com.nonononoki.alovoa.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/matching-analytics")
public class MatchingAnalyticsController {

    private final AuthService authService;
    private final MatchingAnalyticsService analyticsService;
    private final RollingStatsAggregationService rollingStatsAggregationService;

    public MatchingAnalyticsController(AuthService authService,
                                      MatchingAnalyticsService analyticsService,
                                      RollingStatsAggregationService rollingStatsAggregationService) {
        this.authService = authService;
        this.analyticsService = analyticsService;
        this.rollingStatsAggregationService = rollingStatsAggregationService;
    }

    @GetMapping("/summary")
    public ResponseEntity<?> summary(@RequestParam(required = false) String from,
                                     @RequestParam(required = false) String to) {
        try {
            User user = authService.getCurrentUser(true);
            if (!user.isAdmin()) {
                return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
            }

            Instant end = parseOrDefault(to, Instant.now());
            Instant start = parseOrDefault(from, end.minus(7, ChronoUnit.DAYS));
            return ResponseEntity.ok(analyticsService.summary(start, end));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/distribution-shift")
    public ResponseEntity<?> distributionShift(@RequestParam String preFrom,
                                               @RequestParam String preTo,
                                               @RequestParam String postFrom,
                                               @RequestParam String postTo) {
        try {
            User user = authService.getCurrentUser(true);
            if (!user.isAdmin()) {
                return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
            }

            Instant a = Instant.parse(preFrom);
            Instant b = Instant.parse(preTo);
            Instant c = Instant.parse(postFrom);
            Instant d = Instant.parse(postTo);

            return ResponseEntity.ok(analyticsService.distributionShift(a, b, c, d));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/rebuild-stats")
    public ResponseEntity<?> rebuildStats() {
        try {
            User user = authService.getCurrentUser(true);
            if (!user.isAdmin()) {
                return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
            }

            rollingStatsAggregationService.rebuildRollingStats();
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    private Instant parseOrDefault(String value, Instant fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Instant.parse(value);
    }
}
