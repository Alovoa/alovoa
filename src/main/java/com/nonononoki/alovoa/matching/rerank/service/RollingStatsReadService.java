package com.nonononoki.alovoa.matching.rerank.service;

import com.nonononoki.alovoa.entity.matching.UserRollingStats;
import com.nonononoki.alovoa.matching.rerank.model.UserStatsSnapshot;
import com.nonononoki.alovoa.repo.matching.UserRollingStatsRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RollingStatsReadService {

    private final UserRollingStatsRepository rollingStatsRepo;
    private final JdbcTemplate jdbcTemplate;

    public RollingStatsReadService(UserRollingStatsRepository rollingStatsRepo,
                                   JdbcTemplate jdbcTemplate) {
        this.rollingStatsRepo = rollingStatsRepo;
        this.jdbcTemplate = jdbcTemplate;
    }

    public UserStatsSnapshot getSnapshot(Long userId, String segmentKey) {
        UserStatsSnapshot base = rollingStatsRepo.findByUserIdAndSegmentKey(userId, segmentKey)
                .map(this::toSnapshot)
                .orElse(UserStatsSnapshot.empty(userId, segmentKey));
        Map<Long, CollaborativePrior> priors = loadCollaborativePriors(List.of(userId), segmentKey);
        return applyCollaborativePrior(base, priors.get(userId));
    }

    public Map<Long, UserStatsSnapshot> getSnapshots(Collection<Long> userIds, String segmentKey) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        List<UserRollingStats> statsRows = rollingStatsRepo.findByUserIdInAndSegmentKey(userIds, segmentKey);
        Map<Long, UserStatsSnapshot> out = statsRows.stream()
                .map(this::toSnapshot)
                .collect(Collectors.toMap(UserStatsSnapshot::getUserId, Function.identity(), (a, b) -> a));

        for (Long id : userIds) {
            out.putIfAbsent(id, UserStatsSnapshot.empty(id, segmentKey));
        }

        Map<Long, CollaborativePrior> priors = loadCollaborativePriors(userIds, segmentKey);
        for (Long id : userIds) {
            out.put(id, applyCollaborativePrior(out.get(id), priors.get(id)));
        }

        return out;
    }

    public Map<Integer, Integer> getViewerBucketImpressions(Long viewerId) {
        Date since = Date.from(Instant.now().minus(7, ChronoUnit.DAYS));
        List<Map.Entry<Integer, Integer>> rows = jdbcTemplate.query(
                "SELECT COALESCE(candidate_desirability_decile, 5) AS decile, COUNT(*) AS c " +
                        "FROM impression_events " +
                        "WHERE viewer_id = ? AND ts >= ? " +
                        "GROUP BY COALESCE(candidate_desirability_decile, 5)",
                (rs, rowNum) -> Map.entry(rs.getInt("decile"), rs.getInt("c")),
                viewerId,
                new java.sql.Timestamp(since.getTime())
        );

        Map<Integer, Integer> out = new HashMap<>();
        for (Map.Entry<Integer, Integer> row : rows) {
            out.put(row.getKey(), row.getValue());
        }
        return out;
    }

    private UserStatsSnapshot toSnapshot(UserRollingStats s) {
        return UserStatsSnapshot.builder()
                .userId(s.getUserId())
                .segmentKey(s.getSegmentKey())
                .impressions7d(s.getImpressions7d())
                .inboundLikes7d(s.getInboundLikes7d())
                .outboundLikes7d(s.getOutboundLikes7d())
                .matches7d(s.getMatches7d())
                .openMatches(s.getOpenMatches())
                .unreadThreads(s.getUnreadThreads())
                .pendingInboundLikes(s.getPendingInboundLikes())
                .a7d(s.getA7d())
                .dPercentile7d(s.getDPercentile7d())
                .backendAttractivenessScore(s.getBackendAttractivenessScore())
                .collaborativePrior(0.5)
                .collaborativeConfidence(0.0)
                .build();
    }

    private Map<Long, CollaborativePrior> loadCollaborativePriors(Collection<Long> userIds, String segmentKey) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        List<Long> ids = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }

        String placeholders = ids.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "SELECT user_id, segment_key, prior_score, confidence " +
                "FROM user_collaborative_prior " +
                "WHERE user_id IN (" + placeholders + ") " +
                "AND segment_key IN (?, '*') " +
                "ORDER BY user_id, CASE WHEN segment_key = ? THEN 0 ELSE 1 END";

        List<Object> params = new ArrayList<>(ids);
        params.add(segmentKey);
        params.add(segmentKey);

        List<Map.Entry<Long, CollaborativePrior>> rows = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> Map.entry(
                        rs.getLong("user_id"),
                        new CollaborativePrior(
                                rs.getDouble("prior_score"),
                                rs.getDouble("confidence")
                        )
                ),
                params.toArray()
        );

        Map<Long, CollaborativePrior> out = new HashMap<>();
        for (Map.Entry<Long, CollaborativePrior> row : rows) {
            out.putIfAbsent(row.getKey(), row.getValue());
        }
        return out;
    }

    private UserStatsSnapshot applyCollaborativePrior(UserStatsSnapshot base, CollaborativePrior prior) {
        if (base == null) {
            return null;
        }
        double priorScore = prior == null ? 0.5 : clamp01(prior.priorScore());
        double confidence = prior == null ? 0.0 : clamp01(prior.confidence());
        return UserStatsSnapshot.builder()
                .userId(base.getUserId())
                .segmentKey(base.getSegmentKey())
                .impressions7d(base.getImpressions7d())
                .inboundLikes7d(base.getInboundLikes7d())
                .outboundLikes7d(base.getOutboundLikes7d())
                .matches7d(base.getMatches7d())
                .openMatches(base.getOpenMatches())
                .unreadThreads(base.getUnreadThreads())
                .pendingInboundLikes(base.getPendingInboundLikes())
                .a7d(base.getA7d())
                .dPercentile7d(base.getDPercentile7d())
                .backendAttractivenessScore(base.getBackendAttractivenessScore())
                .collaborativePrior(priorScore)
                .collaborativeConfidence(confidence)
                .build();
    }

    private double clamp01(double value) {
        if (value < 0.0) return 0.0;
        if (value > 1.0) return 1.0;
        return value;
    }

    private record CollaborativePrior(double priorScore, double confidence) {
    }
}
