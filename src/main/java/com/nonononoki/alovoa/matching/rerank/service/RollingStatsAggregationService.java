package com.nonononoki.alovoa.matching.rerank.service;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(value = "app.aura.reranker.stats.enabled", havingValue = "true", matchIfMissing = true)
public class RollingStatsAggregationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RollingStatsAggregationService.class);

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepo;
    private final SegmentKeyService segmentKeyService;

    @Value("${app.aura.reranker.n0:200}")
    private double n0;

    @Value("${app.aura.reranker.baseline-attractiveness:0.5}")
    private double baselineAttractiveness;

    @Value("${app.aura.reranker.visual-weight:0.2}")
    private double visualWeight;

    public RollingStatsAggregationService(JdbcTemplate jdbcTemplate,
                                          UserRepository userRepo,
                                          SegmentKeyService segmentKeyService) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepo = userRepo;
        this.segmentKeyService = segmentKeyService;
    }

    @Scheduled(cron = "${app.aura.reranker.stats.nightly-cron:0 30 2 * * *}")
    public void aggregateNightly() {
        rebuildRollingStats();
    }

    @Scheduled(cron = "${app.aura.reranker.stats.hourly-cron:0 15 * * * *}")
    public void aggregateHourly() {
        rebuildRollingStats();
    }

    @Transactional
    public void rebuildRollingStats() {
        Instant windowEnd = Instant.now();
        Instant windowStart = windowEnd.minus(7, ChronoUnit.DAYS);
        Timestamp tsWindowStart = Timestamp.from(windowStart);

        List<User> users = userRepo.findByDisabledFalseAndAdminFalseAndConfirmedTrue();
        if (users.isEmpty()) {
            return;
        }

        Map<Long, Integer> impressions = countByUser(
                "SELECT candidate_id AS user_id, COUNT(*) AS c FROM impression_events WHERE ts >= ? GROUP BY candidate_id",
                tsWindowStart
        );
        Map<Long, Integer> inboundLikes = countByUser(
                "SELECT candidate_id AS user_id, COUNT(*) AS c FROM like_events WHERE ts >= ? GROUP BY candidate_id",
                tsWindowStart
        );
        Map<Long, Integer> outboundLikes = countByUser(
                "SELECT viewer_id AS user_id, COUNT(*) AS c FROM like_events WHERE ts >= ? GROUP BY viewer_id",
                tsWindowStart
        );
        Map<Long, Integer> matches = countByUser(
                "SELECT user_id, COUNT(*) AS c FROM (" +
                        "SELECT user_a AS user_id FROM match_events WHERE ts >= ? " +
                        "UNION ALL " +
                        "SELECT user_b AS user_id FROM match_events WHERE ts >= ?" +
                        ") m GROUP BY user_id",
                tsWindowStart,
                tsWindowStart
        );

        Timestamp openWindowStart = Timestamp.from(windowEnd.minus(30, ChronoUnit.DAYS));
        Map<Long, Integer> openMatches = countByUser(
                "SELECT user_id, COUNT(*) AS c FROM (" +
                        "SELECT user_a_id AS user_id FROM match_window WHERE status = 'CONFIRMED' AND updated_at >= ? " +
                        "UNION ALL " +
                        "SELECT user_b_id AS user_id FROM match_window WHERE status = 'CONFIRMED' AND updated_at >= ?" +
                        ") m GROUP BY user_id",
                openWindowStart,
                openWindowStart
        );

        Map<Long, Integer> unreadThreads = countByUser(
                "SELECT user_to_id AS user_id, COUNT(DISTINCT conversation_id) AS c " +
                        "FROM message WHERE read_at IS NULL GROUP BY user_to_id"
        );

        Map<Long, Integer> pendingInboundLikes = countByUser(
                "SELECT ul.user_to_id AS user_id, COUNT(*) AS c " +
                        "FROM user_like ul " +
                        "LEFT JOIN user_like back " +
                        "  ON back.user_from_id = ul.user_to_id AND back.user_to_id = ul.user_from_id " +
                        "WHERE back.id IS NULL AND ul.date >= ? " +
                        "GROUP BY ul.user_to_id",
                tsWindowStart
        );

        List<RollingStatRow> rows = users.stream()
                .map(u -> buildRow(u, windowStart, windowEnd,
                        impressions, inboundLikes, outboundLikes,
                        matches, openMatches, unreadThreads, pendingInboundLikes))
                .collect(Collectors.toList());

        jdbcTemplate.batchUpdate(
                "INSERT INTO user_rolling_stats (" +
                        "user_id, segment_key, window_start, window_end, " +
                        "impressions_7d, inbound_likes_7d, outbound_likes_7d, matches_7d, " +
                        "open_matches, unread_threads, pending_inbound_likes, " +
                        "A_7d, D_percentile_7d, backend_attractiveness_score" +
                        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0.5, 0.5) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "window_start = VALUES(window_start), " +
                        "window_end = VALUES(window_end), " +
                        "impressions_7d = VALUES(impressions_7d), " +
                        "inbound_likes_7d = VALUES(inbound_likes_7d), " +
                        "outbound_likes_7d = VALUES(outbound_likes_7d), " +
                        "matches_7d = VALUES(matches_7d), " +
                        "open_matches = VALUES(open_matches), " +
                        "unread_threads = VALUES(unread_threads), " +
                        "pending_inbound_likes = VALUES(pending_inbound_likes), " +
                        "A_7d = VALUES(A_7d)",
                rows,
                rows.size(),
                (PreparedStatement ps, RollingStatRow row) -> {
                    ps.setLong(1, row.userId());
                    ps.setString(2, row.segmentKey());
                    ps.setTimestamp(3, Timestamp.from(row.windowStart()));
                    ps.setTimestamp(4, Timestamp.from(row.windowEnd()));
                    ps.setInt(5, row.impressions7d());
                    ps.setInt(6, row.inboundLikes7d());
                    ps.setInt(7, row.outboundLikes7d());
                    ps.setInt(8, row.matches7d());
                    ps.setInt(9, row.openMatches());
                    ps.setInt(10, row.unreadThreads());
                    ps.setInt(11, row.pendingInboundLikes());
                    ps.setDouble(12, row.a7d());
                }
        );

        updatePercentilesAndAttractiveness();

        LOGGER.info("Rolling stats rebuilt for {} users", rows.size());
    }

    private RollingStatRow buildRow(User user,
                                    Instant windowStart,
                                    Instant windowEnd,
                                    Map<Long, Integer> impressions,
                                    Map<Long, Integer> inboundLikes,
                                    Map<Long, Integer> outboundLikes,
                                    Map<Long, Integer> matches,
                                    Map<Long, Integer> openMatches,
                                    Map<Long, Integer> unreadThreads,
                                    Map<Long, Integer> pendingInboundLikes) {
        long userId = user.getId();
        int imp = impressions.getOrDefault(userId, 0);
        int inbound = inboundLikes.getOrDefault(userId, 0);
        int outbound = outboundLikes.getOrDefault(userId, 0);
        int matchCount = matches.getOrDefault(userId, 0);
        int open = openMatches.getOrDefault(userId, 0);
        int unread = unreadThreads.getOrDefault(userId, 0);
        int pendingLikes = pendingInboundLikes.getOrDefault(userId, 0);

        double a7d = imp > 0 ? ((double) inbound / (double) imp) : 0.0;
        String segmentKey = segmentKeyService.segmentKey(user);

        return new RollingStatRow(
                userId,
                segmentKey,
                windowStart,
                windowEnd,
                imp,
                inbound,
                outbound,
                matchCount,
                open,
                unread,
                pendingLikes,
                a7d
        );
    }

    private Map<Long, Integer> countByUser(String sql, Object... params) {
        return jdbcTemplate.query(sql, (rs, rowNum) -> Map.entry(rs.getLong("user_id"), rs.getInt("c")), params)
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Integer::sum));
    }

    private void updatePercentilesAndAttractiveness() {
        // Compute D(u) percentile per segment from A(u).
        jdbcTemplate.update(
                "UPDATE user_rolling_stats urs " +
                        "JOIN (" +
                        "  SELECT ranked.id, " +
                        "         CASE " +
                        "           WHEN ranked.cnt <= 1 THEN 0.5 " +
                        "           ELSE (ranked.rnk - 1) / (ranked.cnt - 1) " +
                        "         END AS pct " +
                        "  FROM (" +
                        "     SELECT id, segment_key, A_7d, " +
                        "            RANK() OVER (PARTITION BY segment_key ORDER BY A_7d) AS rnk, " +
                        "            COUNT(*) OVER (PARTITION BY segment_key) AS cnt " +
                        "     FROM user_rolling_stats" +
                        "  ) ranked" +
                        ") p ON p.id = urs.id " +
                        "SET urs.D_percentile_7d = p.pct"
        );

        // Hidden backend attractiveness score with cold-start smoothing.
        double safeN0 = Math.max(1.0, n0);
        double baseline = Math.max(0.0, Math.min(1.0, baselineAttractiveness));
        double clampedVisualWeight = Math.max(0.0, Math.min(1.0, visualWeight));

        jdbcTemplate.update(
                "UPDATE user_rolling_stats urs " +
                        "LEFT JOIN user_visual_attractiveness uva ON uva.user_id = urs.user_id " +
                        "SET urs.backend_attractiveness_score = LEAST(1.0, GREATEST(0.0, " +
                        "   ((urs.impressions_7d * urs.D_percentile_7d + ? * ?) / (urs.impressions_7d + ?)) * " +
                        "   (1.0 - (? * COALESCE(uva.confidence, 0.0))) + " +
                        "   COALESCE(uva.visual_score, ?) * (? * COALESCE(uva.confidence, 0.0))" +
                        "))",
                safeN0,
                baseline,
                safeN0,
                clampedVisualWeight,
                baseline,
                clampedVisualWeight
        );
    }

    private record RollingStatRow(
            long userId,
            String segmentKey,
            Instant windowStart,
            Instant windowEnd,
            int impressions7d,
            int inboundLikes7d,
            int outboundLikes7d,
            int matches7d,
            int openMatches,
            int unreadThreads,
            int pendingInboundLikes,
            double a7d
    ) {
    }
}
