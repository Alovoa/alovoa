package com.nonononoki.alovoa.matching.rerank.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class ImpressionEventBatchWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImpressionEventBatchWriter.class);

    private final JdbcTemplate jdbcTemplate;
    private final LinkedBlockingQueue<ImpressionEventRecord> queue;
    private final int batchSize;

    public ImpressionEventBatchWriter(JdbcTemplate jdbcTemplate,
                                      @Value("${app.aura.reranker.impression.batch-size:200}") int batchSize,
                                      @Value("${app.aura.reranker.impression.queue-size:50000}") int queueSize) {
        this.jdbcTemplate = jdbcTemplate;
        this.batchSize = Math.max(1, batchSize);
        this.queue = new LinkedBlockingQueue<>(Math.max(1000, queueSize));
    }

    public void enqueue(List<ImpressionEventRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }

        for (ImpressionEventRecord record : records) {
            if (!queue.offer(record)) {
                flushBatchInternal(batchSize * 2);
                if (!queue.offer(record)) {
                    // Guaranteed fallback: do not drop event if queue remains full.
                    writeDirect(record);
                }
            }
        }

        if (queue.size() >= batchSize) {
            flushBatchInternal(batchSize);
        }
    }

    @Scheduled(fixedDelayString = "${app.aura.reranker.impression.flush-ms:2000}")
    public void scheduledFlush() {
        flushBatchInternal(batchSize);
    }

    private synchronized void flushBatchInternal(int targetSize) {
        List<ImpressionEventRecord> drained = new ArrayList<>(targetSize);
        queue.drainTo(drained, Math.max(1, targetSize));
        if (drained.isEmpty()) {
            return;
        }

        try {
            jdbcTemplate.batchUpdate(
                    "INSERT INTO impression_events " +
                            "(ts, viewer_id, candidate_id, surface, position, segment_key, request_id, candidate_desirability_decile, variant) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    drained,
                    drained.size(),
                    (PreparedStatement ps, ImpressionEventRecord r) -> {
                        ps.setTimestamp(1, Timestamp.from(r.ts()));
                        ps.setLong(2, r.viewerId());
                        ps.setLong(3, r.candidateId());
                        ps.setString(4, r.surface());
                        ps.setInt(5, r.position());
                        ps.setString(6, r.segmentKey());
                        ps.setString(7, r.requestId());
                        if (r.candidateDesirabilityDecile() == null) {
                            ps.setNull(8, java.sql.Types.TINYINT);
                        } else {
                            ps.setInt(8, r.candidateDesirabilityDecile());
                        }
                        ps.setString(9, r.variant());
                    }
            );
        } catch (Exception e) {
            LOGGER.error("Failed to batch-write impression events. Falling back to direct writes.", e);
            for (ImpressionEventRecord record : drained) {
                writeDirect(record);
            }
        }
    }

    private void writeDirect(ImpressionEventRecord record) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO impression_events " +
                            "(ts, viewer_id, candidate_id, surface, position, segment_key, request_id, candidate_desirability_decile, variant) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    Timestamp.from(record.ts()),
                    record.viewerId(),
                    record.candidateId(),
                    record.surface(),
                    record.position(),
                    record.segmentKey(),
                    record.requestId(),
                    record.candidateDesirabilityDecile(),
                    record.variant()
            );
        } catch (Exception e) {
            LOGGER.error("Failed direct-write for impression event viewer={} candidate={}",
                    record.viewerId(), record.candidateId(), e);
        }
    }

    public record ImpressionEventRecord(
            Instant ts,
            long viewerId,
            long candidateId,
            String surface,
            int position,
            String segmentKey,
            String requestId,
            Integer candidateDesirabilityDecile,
            String variant
    ) {
        public static ImpressionEventRecord now(long viewerId,
                                                long candidateId,
                                                String surface,
                                                int position,
                                                String segmentKey,
                                                String requestId,
                                                Integer candidateDesirabilityDecile,
                                                String variant) {
            return new ImpressionEventRecord(
                    Instant.now(),
                    viewerId,
                    candidateId,
                    surface,
                    position,
                    segmentKey,
                    requestId,
                    candidateDesirabilityDecile,
                    variant
            );
        }
    }
}
