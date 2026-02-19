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

-- Variant-level conversion and safety (control vs treatment)
SELECT
    ie.variant,
    COUNT(*) AS impressions,
    SUM(CASE WHEN COALESCE(ie.candidate_desirability_decile, 0) >= 9 THEN 1 ELSE 0 END) / COUNT(*) AS top_decile_impression_share
FROM impression_events ie
WHERE ie.ts BETWEEN :from AND :to
  AND (:segment = '*' OR ie.segment_key = :segment)
GROUP BY ie.variant;

-- Approximate variant mapping by latest viewer exposure in the same window
WITH latest_variant AS (
    SELECT
        viewer_id,
        SUBSTRING_INDEX(GROUP_CONCAT(variant ORDER BY ts DESC), ',', 1) AS variant
    FROM impression_events
    WHERE ts BETWEEN :from AND :to
      AND (:segment = '*' OR segment_key = :segment)
    GROUP BY viewer_id
)
SELECT
    lv.variant,
    COUNT(*) AS likes,
    COUNT(*) / NULLIF((SELECT COUNT(*) FROM impression_events ie WHERE ie.variant = lv.variant AND ie.ts BETWEEN :from AND :to), 0) AS likes_per_impression
FROM like_events le
JOIN latest_variant lv ON lv.viewer_id = le.viewer_id
WHERE le.ts BETWEEN :from AND :to
  AND (:segment = '*' OR le.segment_key = :segment)
GROUP BY lv.variant;

-- Median time-to-first-conversation by segment (viewer -> candidate directionality collapsed)
WITH first_impression AS (
    SELECT
        ie.segment_key,
        LEAST(ie.viewer_id, ie.candidate_id) AS user_low,
        GREATEST(ie.viewer_id, ie.candidate_id) AS user_high,
        MIN(ie.ts) AS first_impression_ts
    FROM impression_events ie
    WHERE ie.ts BETWEEN :from AND :to
    GROUP BY ie.segment_key, LEAST(ie.viewer_id, ie.candidate_id), GREATEST(ie.viewer_id, ie.candidate_id)
),
first_conversation AS (
    SELECT
        LEAST(me.sender_id, me.receiver_id) AS user_low,
        GREATEST(me.sender_id, me.receiver_id) AS user_high,
        MIN(me.ts) AS first_message_ts
    FROM message_events me
    WHERE me.ts BETWEEN :from AND :to
    GROUP BY LEAST(me.sender_id, me.receiver_id), GREATEST(me.sender_id, me.receiver_id)
),
ttfc AS (
    SELECT
        fi.segment_key,
        TIMESTAMPDIFF(SECOND, fi.first_impression_ts, fc.first_message_ts) AS ttfc_seconds
    FROM first_impression fi
    JOIN first_conversation fc
      ON fc.user_low = fi.user_low
     AND fc.user_high = fi.user_high
    WHERE fc.first_message_ts >= fi.first_impression_ts
)
SELECT
    segment_key,
    COUNT(*) AS pairs_with_conversation,
    ROUND(AVG(ttfc_seconds) / 60.0, 2) AS avg_minutes_to_first_conversation,
    CAST(SUBSTRING_INDEX(
        SUBSTRING_INDEX(
            GROUP_CONCAT(ttfc_seconds ORDER BY ttfc_seconds SEPARATOR ','),
            ',',
            FLOOR((COUNT(*) + 1) / 2)
        ),
        ',',
        -1
    ) AS UNSIGNED) AS median_seconds_to_first_conversation
FROM ttfc
GROUP BY segment_key;

-- Distribution shift pre/post rollout cutover (candidate desirability exposure + conversion)
SELECT
    CASE WHEN ie.ts < :flag_enabled_at THEN 'pre' ELSE 'post' END AS period,
    ie.variant,
    COUNT(*) AS impressions,
    AVG(COALESCE(ie.candidate_desirability_decile, 0)) AS avg_candidate_decile,
    SUM(CASE WHEN COALESCE(ie.candidate_desirability_decile, 0) >= 9 THEN 1 ELSE 0 END) / COUNT(*) AS top_decile_share
FROM impression_events ie
WHERE ie.ts BETWEEN :from AND :to
  AND (:segment = '*' OR ie.segment_key = :segment)
GROUP BY CASE WHEN ie.ts < :flag_enabled_at THEN 'pre' ELSE 'post' END, ie.variant;

-- Pre/post conversion drift by variant (like/match/conversation per impression)
WITH latest_variant AS (
    SELECT
        viewer_id,
        SUBSTRING_INDEX(GROUP_CONCAT(variant ORDER BY ts DESC), ',', 1) AS variant
    FROM impression_events
    WHERE ts BETWEEN :from AND :to
      AND (:segment = '*' OR segment_key = :segment)
    GROUP BY viewer_id
),
exposure AS (
    SELECT
        CASE WHEN ts < :flag_enabled_at THEN 'pre' ELSE 'post' END AS period,
        variant,
        COUNT(*) AS impressions
    FROM impression_events
    WHERE ts BETWEEN :from AND :to
      AND (:segment = '*' OR segment_key = :segment)
    GROUP BY CASE WHEN ts < :flag_enabled_at THEN 'pre' ELSE 'post' END, variant
),
likes AS (
    SELECT
        CASE WHEN le.ts < :flag_enabled_at THEN 'pre' ELSE 'post' END AS period,
        lv.variant,
        COUNT(*) AS likes
    FROM like_events le
    JOIN latest_variant lv ON lv.viewer_id = le.viewer_id
    WHERE le.ts BETWEEN :from AND :to
      AND (:segment = '*' OR le.segment_key = :segment)
    GROUP BY CASE WHEN le.ts < :flag_enabled_at THEN 'pre' ELSE 'post' END, lv.variant
),
matches AS (
    SELECT
        CASE WHEN mp.ts < :flag_enabled_at THEN 'pre' ELSE 'post' END AS period,
        lv.variant,
        COUNT(*) AS matches
    FROM (
        SELECT user_a AS user_id, segment_key, ts FROM match_events
        UNION ALL
        SELECT user_b AS user_id, segment_key, ts FROM match_events
    ) mp
    JOIN latest_variant lv ON lv.viewer_id = mp.user_id
    WHERE mp.ts BETWEEN :from AND :to
      AND (:segment = '*' OR mp.segment_key = :segment)
    GROUP BY CASE WHEN mp.ts < :flag_enabled_at THEN 'pre' ELSE 'post' END, lv.variant
),
conversations AS (
    SELECT
        CASE WHEN me.ts < :flag_enabled_at THEN 'pre' ELSE 'post' END AS period,
        lv.variant,
        COUNT(DISTINCT me.conversation_id) AS conversations
    FROM message_events me
    JOIN latest_variant lv ON lv.viewer_id = me.sender_id
    WHERE me.ts BETWEEN :from AND :to
    GROUP BY CASE WHEN me.ts < :flag_enabled_at THEN 'pre' ELSE 'post' END, lv.variant
)
SELECT
    e.period,
    e.variant,
    e.impressions,
    COALESCE(l.likes, 0) / NULLIF(e.impressions, 0) AS likes_per_impression,
    COALESCE(m.matches, 0) / NULLIF(e.impressions, 0) AS matches_per_impression,
    COALESCE(c.conversations, 0) / NULLIF(e.impressions, 0) AS conversations_per_impression
FROM exposure e
LEFT JOIN likes l ON l.period = e.period AND l.variant = e.variant
LEFT JOIN matches m ON m.period = e.period AND m.variant = e.variant
LEFT JOIN conversations c ON c.period = e.period AND c.variant = e.variant
ORDER BY e.period, e.variant;
