package com.nonononoki.alovoa.matching.rerank.service;

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

@Service
public class MatchingEventIngestionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MatchingEventIngestionService.class);

    private final JdbcTemplate jdbcTemplate;
    private final SegmentKeyService segmentKeyService;
    private final ImpressionEventBatchWriter impressionBatchWriter;

    public MatchingEventIngestionService(JdbcTemplate jdbcTemplate,
                                         SegmentKeyService segmentKeyService,
                                         ImpressionEventBatchWriter impressionBatchWriter) {
        this.jdbcTemplate = jdbcTemplate;
        this.segmentKeyService = segmentKeyService;
        this.impressionBatchWriter = impressionBatchWriter;
    }

    public void recordLike(User viewer, User candidate) {
        if (viewer == null || candidate == null || viewer.getId() == null || candidate.getId() == null) {
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
            LOGGER.warn("Failed to write like event viewer={} candidate={}", viewer.getId(), candidate.getId(), e);
        }
    }

    public void recordMatch(User userA, User userB) {
        if (userA == null || userB == null || userA.getId() == null || userB.getId() == null) {
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
            LOGGER.warn("Failed to write match event users=({}, {})", userA.getId(), userB.getId(), e);
        }
    }

    public void recordMessage(User sender, User receiver, Long conversationId) {
        if (sender == null || receiver == null || sender.getId() == null || receiver.getId() == null) {
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
    }
}
