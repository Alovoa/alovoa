package com.nonononoki.alovoa.matching.rerank.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.matching.rerank.model.MatchingRequestContext;
import com.nonononoki.alovoa.matching.rerank.model.ScoreTrace;
import com.nonononoki.alovoa.model.MatchRecommendationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class MatchingEventIngestionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MatchingEventIngestionService.class);

    private final JdbcTemplate jdbcTemplate;
    private final SegmentKeyService segmentKeyService;
    private final ImpressionEventBatchWriter impressionBatchWriter;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean likeEventsEnabled = new AtomicBoolean(true);
    private final AtomicBoolean matchEventsEnabled = new AtomicBoolean(true);
    private final AtomicBoolean messageEventsEnabled = new AtomicBoolean(true);
    private final AtomicBoolean scoreTraceEventsEnabled = new AtomicBoolean(true);

    public MatchingEventIngestionService(JdbcTemplate jdbcTemplate,
                                         SegmentKeyService segmentKeyService,
                                         ImpressionEventBatchWriter impressionBatchWriter,
                                         ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.segmentKeyService = segmentKeyService;
        this.impressionBatchWriter = impressionBatchWriter;
        this.objectMapper = objectMapper;
    }

    public void recordLike(User viewer, User candidate) {
        if (viewer == null || candidate == null || viewer.getId() == null || candidate.getId() == null) {
            return;
        }
        if (!likeEventsEnabled.get()) {
            return;
        }

        try {
            jdbcTemplate.update(
                    "INSERT INTO like_events (ts, viewer_id, candidate_id, direction, segment_key) VALUES (?, ?, ?, ?, ?)",
                    Timestamp.from(Instant.now()),
                    viewer.getId(),
                    candidate.getId(),
                    "viewer_liked_candidate",
                    segmentKeyService.segmentKey(viewer)
            );
        } catch (Exception e) {
            if (disableIfMissingTable(e, "like_events", likeEventsEnabled)) {
                return;
            }
            LOGGER.warn("Failed to write like event viewer={} candidate={}", viewer.getId(), candidate.getId(), e);
        }
    }

    public void recordMatch(User userA, User userB) {
        if (userA == null || userB == null || userA.getId() == null || userB.getId() == null) {
            return;
        }
        if (!matchEventsEnabled.get()) {
            return;
        }

        try {
            jdbcTemplate.update(
                    "INSERT INTO match_events (ts, user_a, user_b, segment_key) VALUES (?, ?, ?, ?)",
                    Timestamp.from(Instant.now()),
                    userA.getId(),
                    userB.getId(),
                    segmentKeyService.segmentKey(userA)
            );
        } catch (Exception e) {
            if (disableIfMissingTable(e, "match_events", matchEventsEnabled)) {
                return;
            }
            LOGGER.warn("Failed to write match event users=({}, {})", userA.getId(), userB.getId(), e);
        }
    }

    public void recordMessage(User sender, User receiver, Long conversationId) {
        if (sender == null || receiver == null || sender.getId() == null || receiver.getId() == null) {
            return;
        }
        if (!messageEventsEnabled.get()) {
            return;
        }

        try {
            jdbcTemplate.update(
                    "INSERT INTO message_events (ts, sender_id, receiver_id, conversation_id) VALUES (?, ?, ?, ?)",
                    Timestamp.from(Instant.now()),
                    sender.getId(),
                    receiver.getId(),
                    conversationId
            );
        } catch (Exception e) {
            if (disableIfMissingTable(e, "message_events", messageEventsEnabled)) {
                return;
            }
            LOGGER.warn("Failed to write message event sender={} receiver={} conversation={}",
                    sender.getId(), receiver.getId(), conversationId, e);
        }
    }

    public void recordImpressions(User viewer,
                                  List<MatchRecommendationDto> ranked,
                                  Map<Long, ScoreTrace> traces,
                                  MatchingRequestContext context) {
        if (viewer == null || viewer.getId() == null || ranked == null || ranked.isEmpty()) {
            return;
        }

        String segment = context != null && context.getSegmentKey() != null
                ? context.getSegmentKey()
                : segmentKeyService.segmentKey(viewer);
        String surface = context != null && context.getSurface() != null ? context.getSurface() : "daily_matches";
        String requestId = context != null ? context.getRequestId() : null;
        String variant = context != null && context.getVariant() != null ? context.getVariant() : "control";

        List<ImpressionEventBatchWriter.ImpressionEventRecord> rows = new ArrayList<>();
        for (int i = 0; i < ranked.size(); i++) {
            MatchRecommendationDto dto = ranked.get(i);
            if (dto == null || dto.getUserId() == null) {
                continue;
            }
            ScoreTrace trace = traces != null ? traces.get(dto.getUserId()) : null;
            Integer decile = trace != null ? trace.getDesirabilityDecile() : null;
            rows.add(ImpressionEventBatchWriter.ImpressionEventRecord.now(
                    viewer.getId(),
                    dto.getUserId(),
                    surface,
                    i,
                    segment,
                    requestId,
                    decile,
                    variant
            ));
        }

        try {
            impressionBatchWriter.enqueue(rows);
        } catch (Exception e) {
            LOGGER.warn("Failed to queue impression events viewer={} requestId={}", viewer.getId(), requestId, e);
        }

        recordScoreTraces(viewer.getId(), ranked, traces, segment, requestId, variant);
    }

    public Optional<Map<String, Object>> latestScoreTrace(long viewerId, long candidateId) {
        if (!scoreTraceEventsEnabled.get()) {
            return Optional.empty();
        }

        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT ts, request_id, viewer_id, candidate_id, segment_key, variant, " +
                            "s, f_exposure, f_capacity, f_gap, f_collaborative, ucb, final_score, desirability_decile, window_stats_json " +
                            "FROM reranker_score_trace_events " +
                            "WHERE viewer_id = ? AND candidate_id = ? " +
                            "ORDER BY ts DESC LIMIT 1",
                    viewerId,
                    candidateId
            );
            if (rows.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(rows.get(0));
        } catch (Exception e) {
            if (disableIfMissingTable(e, "reranker_score_trace_events", scoreTraceEventsEnabled)) {
                return Optional.empty();
            }
            LOGGER.warn("Failed to load reranker score trace viewer={} candidate={}", viewerId, candidateId, e);
            return Optional.empty();
        }
    }

    public List<Map<String, Object>> requestScoreTraces(long viewerId, String requestId, int limit) {
        if (!scoreTraceEventsEnabled.get() || requestId == null || requestId.isBlank()) {
            return List.of();
        }
        int safeLimit = Math.max(1, Math.min(100, limit));

        try {
            return jdbcTemplate.queryForList(
                    "SELECT ts, request_id, viewer_id, candidate_id, segment_key, variant, " +
                            "s, f_exposure, f_capacity, f_gap, f_collaborative, ucb, final_score, desirability_decile, window_stats_json " +
                            "FROM reranker_score_trace_events " +
                            "WHERE viewer_id = ? AND request_id = ? " +
                            "ORDER BY final_score DESC, ts DESC LIMIT ?",
                    viewerId,
                    requestId,
                    safeLimit
            );
        } catch (Exception e) {
            if (disableIfMissingTable(e, "reranker_score_trace_events", scoreTraceEventsEnabled)) {
                return List.of();
            }
            LOGGER.warn("Failed to load reranker request traces viewer={} requestId={}", viewerId, requestId, e);
            return List.of();
        }
    }

    private void recordScoreTraces(long viewerId,
                                   List<MatchRecommendationDto> ranked,
                                   Map<Long, ScoreTrace> traces,
                                   String segment,
                                   String requestId,
                                   String variant) {
        if (!scoreTraceEventsEnabled.get() || ranked == null || ranked.isEmpty() || traces == null || traces.isEmpty()) {
            return;
        }

        List<Object[]> rows = new ArrayList<>();
        for (MatchRecommendationDto dto : ranked) {
            if (dto == null || dto.getUserId() == null) {
                continue;
            }
            ScoreTrace trace = traces.get(dto.getUserId());
            if (trace == null) {
                continue;
            }
            rows.add(new Object[] {
                    Timestamp.from(Instant.now()),
                    requestId,
                    viewerId,
                    dto.getUserId(),
                    segment,
                    variant,
                    trace.getS(),
                    trace.getFExposure(),
                    trace.getFCapacity(),
                    trace.getFGap(),
                    trace.getFCollaborative(),
                    trace.getUcb(),
                    trace.getFinalScore(),
                    trace.getDesirabilityDecile(),
                    toJson(trace.getWindowStats())
            });
        }

        if (rows.isEmpty()) {
            return;
        }

        try {
            jdbcTemplate.batchUpdate(
                    "INSERT INTO reranker_score_trace_events " +
                            "(ts, request_id, viewer_id, candidate_id, segment_key, variant, " +
                            "s, f_exposure, f_capacity, f_gap, f_collaborative, ucb, final_score, desirability_decile, window_stats_json) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    rows
            );
        } catch (Exception e) {
            if (disableIfMissingTable(e, "reranker_score_trace_events", scoreTraceEventsEnabled)) {
                return;
            }
            LOGGER.warn("Failed to persist reranker score traces viewer={} requestId={}", viewerId, requestId, e);
        }
    }

    private String toJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            LOGGER.debug("Failed to serialize score trace window stats", e);
            return "{}";
        }
    }

    private boolean disableIfMissingTable(Exception error, String tableName, AtomicBoolean flag) {
        Throwable cursor = error;
        String marker = tableName.toLowerCase();
        while (cursor != null) {
            String message = cursor.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase();
                boolean tableMissing = normalized.contains("table \"" + marker + "\" not found")
                        || normalized.contains("relation \"" + marker + "\" does not exist");
                if (tableMissing) {
                    if (flag.compareAndSet(true, false)) {
                        LOGGER.warn("Disabling {} event writes because table '{}' is missing in this environment.", tableName, tableName);
                    }
                    return true;
                }
            }
            cursor = cursor.getCause();
        }
        return false;
    }
}
