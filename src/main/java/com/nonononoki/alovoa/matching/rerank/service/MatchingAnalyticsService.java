package com.nonononoki.alovoa.matching.rerank.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MatchingAnalyticsService {

    private final JdbcTemplate jdbcTemplate;

    public MatchingAnalyticsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> summary(Instant from, Instant to) {
        Timestamp tsFrom = Timestamp.from(from);
        Timestamp tsTo = Timestamp.from(to);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("from", from.toString());
        out.put("to", to.toString());
        out.put("impressionConcentration", impressionConcentration(tsFrom, tsTo));
        out.put("matchConcentration", matchConcentration(tsFrom, tsTo));
        out.put("conversationConcentration", conversationConcentration(tsFrom, tsTo));
        out.put("medianTimeToFirstConversationMinutes", medianTimeToFirstConversation(tsFrom, tsTo));
        out.put("conversionPerImpression", conversionPerImpression(tsFrom, tsTo));
        out.put("safetyGuardrails", safetyGuardrails(tsFrom, tsTo));
        return out;
    }

    public Map<String, Object> distributionShift(Instant preFrom,
                                                 Instant preTo,
                                                 Instant postFrom,
                                                 Instant postTo) {
        Map<String, Double> pre = topDecileImpressionShare(Timestamp.from(preFrom), Timestamp.from(preTo));
        Map<String, Double> post = topDecileImpressionShare(Timestamp.from(postFrom), Timestamp.from(postTo));

        Set<String> segments = new TreeSet<>();
        segments.addAll(pre.keySet());
        segments.addAll(post.keySet());

        List<Map<String, Object>> rows = new ArrayList<>();
        for (String segment : segments) {
            double preShare = pre.getOrDefault(segment, 0.0);
            double postShare = post.getOrDefault(segment, 0.0);
            rows.add(Map.of(
                    "segment", segment,
                    "preTopDecileImpressionShare", preShare,
                    "postTopDecileImpressionShare", postShare,
                    "delta", postShare - preShare
            ));
        }

        return Map.of(
                "preFrom", preFrom.toString(),
                "preTo", preTo.toString(),
                "postFrom", postFrom.toString(),
                "postTo", postTo.toString(),
                "rows", rows
        );
    }

    private List<Map<String, Object>> impressionConcentration(Timestamp from, Timestamp to) {
        String sql = "SELECT ie.segment_key AS segment_key, " +
                "SUM(CASE WHEN COALESCE(urs.D_percentile_7d, 0.5) >= 0.9 THEN 1 ELSE 0 END) / COUNT(*) AS share_top_decile, " +
                "COUNT(*) AS impressions " +
                "FROM impression_events ie " +
                "LEFT JOIN user_rolling_stats urs " +
                "  ON urs.user_id = ie.candidate_id AND urs.segment_key = ie.segment_key " +
                "WHERE ie.ts BETWEEN ? AND ? " +
                "GROUP BY ie.segment_key " +
                "ORDER BY share_top_decile DESC";
        return jdbcTemplate.queryForList(sql, from, to);
    }

    private List<Map<String, Object>> matchConcentration(Timestamp from, Timestamp to) {
        String sql = "SELECT m.segment_key AS segment_key, " +
                "SUM(CASE WHEN COALESCE(urs.D_percentile_7d, 0.5) >= 0.9 THEN 1 ELSE 0 END) / COUNT(*) AS share_top_decile, " +
                "COUNT(*) AS match_participations " +
                "FROM (" +
                "  SELECT segment_key, user_a AS user_id FROM match_events WHERE ts BETWEEN ? AND ? " +
                "  UNION ALL " +
                "  SELECT segment_key, user_b AS user_id FROM match_events WHERE ts BETWEEN ? AND ? " +
                ") m " +
                "LEFT JOIN user_rolling_stats urs " +
                "  ON urs.user_id = m.user_id AND urs.segment_key = m.segment_key " +
                "GROUP BY m.segment_key " +
                "ORDER BY share_top_decile DESC";
        return jdbcTemplate.queryForList(sql, from, to, from, to);
    }

    private List<Map<String, Object>> conversationConcentration(Timestamp from, Timestamp to) {
        String sql = "SELECT urs.segment_key AS segment_key, " +
                "SUM(CASE WHEN COALESCE(urs.D_percentile_7d, 0.5) >= 0.9 THEN 1 ELSE 0 END) / COUNT(*) AS share_top_decile, " +
                "COUNT(*) AS active_conversation_participations " +
                "FROM (" +
                "  SELECT DISTINCT conversation_id, sender_id AS user_id FROM message_events WHERE ts BETWEEN ? AND ? " +
                "  UNION " +
                "  SELECT DISTINCT conversation_id, receiver_id AS user_id FROM message_events WHERE ts BETWEEN ? AND ? " +
                ") c " +
                "LEFT JOIN user_rolling_stats urs ON urs.user_id = c.user_id " +
                "GROUP BY urs.segment_key " +
                "ORDER BY share_top_decile DESC";
        return jdbcTemplate.queryForList(sql, from, to, from, to);
    }

    private Map<String, Double> topDecileImpressionShare(Timestamp from, Timestamp to) {
        String sql = "SELECT ie.segment_key AS segment_key, " +
                "SUM(CASE WHEN COALESCE(urs.D_percentile_7d, 0.5) >= 0.9 THEN 1 ELSE 0 END) / COUNT(*) AS share_top_decile " +
                "FROM impression_events ie " +
                "LEFT JOIN user_rolling_stats urs " +
                "  ON urs.user_id = ie.candidate_id AND urs.segment_key = ie.segment_key " +
                "WHERE ie.ts BETWEEN ? AND ? " +
                "GROUP BY ie.segment_key";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, from, to);
        Map<String, Double> out = new HashMap<>();
        for (Map<String, Object> row : rows) {
            out.put(String.valueOf(row.get("segment_key")), number(row.get("share_top_decile")));
        }
        return out;
    }

    private List<Map<String, Object>> medianTimeToFirstConversation(Timestamp from, Timestamp to) {
        String sql = "SELECT m.segment_key AS segment_key, " +
                "TIMESTAMPDIFF(MINUTE, m.ts, (" +
                "  SELECT MIN(me.ts) FROM message_events me " +
                "  WHERE me.ts >= m.ts AND ((me.sender_id = m.user_a AND me.receiver_id = m.user_b) " +
                "     OR (me.sender_id = m.user_b AND me.receiver_id = m.user_a))" +
                ")) AS minutes_to_first " +
                "FROM match_events m " +
                "WHERE m.ts BETWEEN ? AND ?";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, from, to);

        Map<String, List<Double>> grouped = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String segment = String.valueOf(row.get("segment_key"));
            Double minutes = row.get("minutes_to_first") == null ? null : number(row.get("minutes_to_first"));
            if (minutes == null || minutes < 0) {
                continue;
            }
            grouped.computeIfAbsent(segment, k -> new ArrayList<>()).add(minutes);
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (Map.Entry<String, List<Double>> entry : grouped.entrySet()) {
            List<Double> values = entry.getValue().stream().sorted().collect(Collectors.toList());
            double median;
            int size = values.size();
            if (size == 0) {
                median = 0.0;
            } else if (size % 2 == 1) {
                median = values.get(size / 2);
            } else {
                median = (values.get((size / 2) - 1) + values.get(size / 2)) / 2.0;
            }
            out.add(Map.of(
                    "segment", entry.getKey(),
                    "medianMinutes", median,
                    "sampleSize", size
            ));
        }

        out.sort(Comparator.comparing(o -> String.valueOf(o.get("segment"))));
        return out;
    }

    private List<Map<String, Object>> conversionPerImpression(Timestamp from, Timestamp to) {
        String sql = "SELECT s.segment_key, " +
                "COALESCE(i.impressions, 0) AS impressions, " +
                "COALESCE(l.likes, 0) AS likes, " +
                "COALESCE(m.matches, 0) AS matches, " +
                "COALESCE(c.conversations, 0) AS conversations " +
                "FROM (" +
                "  SELECT segment_key FROM impression_events WHERE ts BETWEEN ? AND ? " +
                "  UNION SELECT segment_key FROM like_events WHERE ts BETWEEN ? AND ? " +
                "  UNION SELECT segment_key FROM match_events WHERE ts BETWEEN ? AND ? " +
                "  UNION SELECT segment_key FROM user_rolling_stats" +
                ") s " +
                "LEFT JOIN (SELECT segment_key, COUNT(*) AS impressions FROM impression_events WHERE ts BETWEEN ? AND ? GROUP BY segment_key) i " +
                "  ON i.segment_key = s.segment_key " +
                "LEFT JOIN (SELECT segment_key, COUNT(*) AS likes FROM like_events WHERE ts BETWEEN ? AND ? GROUP BY segment_key) l " +
                "  ON l.segment_key = s.segment_key " +
                "LEFT JOIN (SELECT segment_key, COUNT(*) AS matches FROM match_events WHERE ts BETWEEN ? AND ? GROUP BY segment_key) m " +
                "  ON m.segment_key = s.segment_key " +
                "LEFT JOIN (" +
                "  SELECT urs.segment_key, COUNT(DISTINCT me.conversation_id) AS conversations " +
                "  FROM message_events me " +
                "  LEFT JOIN user_rolling_stats urs ON urs.user_id = me.sender_id " +
                "  WHERE me.ts BETWEEN ? AND ? " +
                "  GROUP BY urs.segment_key" +
                ") c ON c.segment_key = s.segment_key";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                sql,
                from, to,
                from, to,
                from, to,
                from, to,
                from, to,
                from, to,
                from, to
        );

        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String segment = String.valueOf(row.get("segment_key"));
            double impressions = number(row.get("impressions"));
            double likes = number(row.get("likes"));
            double matches = number(row.get("matches"));
            double conversations = number(row.get("conversations"));

            double safeImpressions = impressions <= 0 ? 1.0 : impressions;
            out.add(Map.of(
                    "segment", segment,
                    "impressions", impressions,
                    "likesPerImpression", likes / safeImpressions,
                    "matchesPerImpression", matches / safeImpressions,
                    "conversationsPerImpression", conversations / safeImpressions
            ));
        }

        out.sort(Comparator.comparing(o -> String.valueOf(o.get("segment"))));
        return out;
    }

    private double number(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0.0;
    }

    private Map<String, Object> safetyGuardrails(Timestamp from, Timestamp to) {
        Number impressionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM impression_events WHERE ts BETWEEN ? AND ?",
                Number.class,
                from,
                to
        );
        Number reportCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_report WHERE date BETWEEN ? AND ?",
                Number.class,
                from,
                to
        );
        Number blockCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_block WHERE date BETWEEN ? AND ?",
                Number.class,
                from,
                to
        );

        double impressions = number(impressionCount);
        double reports = number(reportCount);
        double blocks = number(blockCount);

        double denom = impressions <= 0 ? 1.0 : impressions;
        return Map.of(
                "reportRatePerImpression", reports / denom,
                "blockRatePerImpression", blocks / denom,
                "reports", reports,
                "blocks", blocks,
                "impressions", impressions
        );
    }

    public Instant defaultFrom() {
        return Instant.now().minus(7, ChronoUnit.DAYS);
    }
}
