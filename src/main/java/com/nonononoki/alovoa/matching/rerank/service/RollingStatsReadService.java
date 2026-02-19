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
        return rollingStatsRepo.findByUserIdAndSegmentKey(userId, segmentKey)
                .map(this::toSnapshot)
                .orElse(UserStatsSnapshot.empty(userId, segmentKey));
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
                .build();
    }
}
