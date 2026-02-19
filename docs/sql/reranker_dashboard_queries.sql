-- Impression concentration: share of impressions held by top decile of desirability
SELECT ie.segment_key,
       SUM(CASE WHEN COALESCE(urs.D_percentile_7d, 0.5) >= 0.9 THEN 1 ELSE 0 END) / COUNT(*) AS share_top_decile,
       COUNT(*) AS impressions
FROM impression_events ie
LEFT JOIN user_rolling_stats urs
       ON urs.user_id = ie.candidate_id
      AND urs.segment_key = ie.segment_key
WHERE ie.ts BETWEEN :from AND :to
GROUP BY ie.segment_key;

-- Match concentration: share of match participations held by top decile
SELECT m.segment_key,
       SUM(CASE WHEN COALESCE(urs.D_percentile_7d, 0.5) >= 0.9 THEN 1 ELSE 0 END) / COUNT(*) AS share_top_decile,
       COUNT(*) AS match_participations
FROM (
    SELECT segment_key, user_a AS user_id FROM match_events WHERE ts BETWEEN :from AND :to
    UNION ALL
    SELECT segment_key, user_b AS user_id FROM match_events WHERE ts BETWEEN :from AND :to
) m
LEFT JOIN user_rolling_stats urs
       ON urs.user_id = m.user_id
      AND urs.segment_key = m.segment_key
GROUP BY m.segment_key;

-- Conversation concentration: share of active 7d conversation participations held by top decile
SELECT urs.segment_key,
       SUM(CASE WHEN COALESCE(urs.D_percentile_7d, 0.5) >= 0.9 THEN 1 ELSE 0 END) / COUNT(*) AS share_top_decile,
       COUNT(*) AS active_conversation_participations
FROM (
    SELECT DISTINCT conversation_id, sender_id AS user_id FROM message_events WHERE ts BETWEEN :from AND :to
    UNION
    SELECT DISTINCT conversation_id, receiver_id AS user_id FROM message_events WHERE ts BETWEEN :from AND :to
) c
LEFT JOIN user_rolling_stats urs ON urs.user_id = c.user_id
GROUP BY urs.segment_key;

-- Conversion per impression by segment
SELECT s.segment_key,
       COALESCE(i.impressions, 0) AS impressions,
       COALESCE(l.likes, 0) / NULLIF(COALESCE(i.impressions, 0), 0) AS likes_per_impression,
       COALESCE(m.matches, 0) / NULLIF(COALESCE(i.impressions, 0), 0) AS matches_per_impression,
       COALESCE(c.conversations, 0) / NULLIF(COALESCE(i.impressions, 0), 0) AS conversations_per_impression
FROM (
    SELECT segment_key FROM impression_events WHERE ts BETWEEN :from AND :to
    UNION SELECT segment_key FROM like_events WHERE ts BETWEEN :from AND :to
    UNION SELECT segment_key FROM match_events WHERE ts BETWEEN :from AND :to
) s
LEFT JOIN (
    SELECT segment_key, COUNT(*) AS impressions
    FROM impression_events
    WHERE ts BETWEEN :from AND :to
    GROUP BY segment_key
) i ON i.segment_key = s.segment_key
LEFT JOIN (
    SELECT segment_key, COUNT(*) AS likes
    FROM like_events
    WHERE ts BETWEEN :from AND :to
    GROUP BY segment_key
) l ON l.segment_key = s.segment_key
LEFT JOIN (
    SELECT segment_key, COUNT(*) AS matches
    FROM match_events
    WHERE ts BETWEEN :from AND :to
    GROUP BY segment_key
) m ON m.segment_key = s.segment_key
LEFT JOIN (
    SELECT urs.segment_key, COUNT(DISTINCT me.conversation_id) AS conversations
    FROM message_events me
    LEFT JOIN user_rolling_stats urs ON urs.user_id = me.sender_id
    WHERE me.ts BETWEEN :from AND :to
    GROUP BY urs.segment_key
) c ON c.segment_key = s.segment_key;

-- Global safety guardrails used for A/B auto rollback checks
SELECT
    COUNT(*) AS impressions,
    (SELECT COUNT(*) FROM user_report WHERE date BETWEEN :from AND :to) AS reports,
    (SELECT COUNT(*) FROM user_block WHERE date BETWEEN :from AND :to) AS blocks,
    (SELECT COUNT(*) FROM user_report WHERE date BETWEEN :from AND :to) / NULLIF(COUNT(*), 0) AS report_rate_per_impression,
    (SELECT COUNT(*) FROM user_block WHERE date BETWEEN :from AND :to) / NULLIF(COUNT(*), 0) AS block_rate_per_impression
FROM impression_events
WHERE ts BETWEEN :from AND :to;
