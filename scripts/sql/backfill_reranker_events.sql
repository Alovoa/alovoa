-- Backfill event tables from legacy first-party tables.
-- Safe to run multiple times (deduplicates by key dimensions).

-- 1) Backfill impression_events from profile visits (best available historical proxy).
INSERT INTO impression_events (
    ts,
    viewer_id,
    candidate_id,
    surface,
    position,
    segment_key,
    request_id,
    candidate_desirability_decile,
    variant
)
SELECT
    COALESCE(v.last_visit_at, v.visited_at, NOW()) AS ts,
    v.visitor_id,
    v.visited_user_id,
    'profile_visit_backfill' AS surface,
    0 AS position,
    'legacy' AS segment_key,
    CONCAT('backfill-visit-', v.id) AS request_id,
    NULL AS candidate_desirability_decile,
    'legacy' AS variant
FROM user_profile_visit v
LEFT JOIN impression_events ie
       ON ie.viewer_id = v.visitor_id
      AND ie.candidate_id = v.visited_user_id
      AND ie.request_id = CONCAT('backfill-visit-', v.id)
WHERE ie.id IS NULL;

-- 2) Backfill like_events from user_like.
INSERT INTO like_events (ts, viewer_id, candidate_id, direction, segment_key)
SELECT
    COALESCE(ul.date, NOW()) AS ts,
    ul.user_from_id,
    ul.user_to_id,
    'viewer_liked_candidate' AS direction,
    'legacy' AS segment_key
FROM user_like ul
LEFT JOIN like_events le
       ON le.viewer_id = ul.user_from_id
      AND le.candidate_id = ul.user_to_id
      AND le.ts = ul.date
WHERE le.id IS NULL;

-- 3) Backfill match_events from confirmed match windows.
INSERT INTO match_events (ts, user_a, user_b, segment_key)
SELECT
    COALESCE(w.updated_at, w.created_at, NOW()) AS ts,
    w.user_a_id,
    w.user_b_id,
    'legacy' AS segment_key
FROM match_window w
LEFT JOIN match_events me
       ON me.user_a = w.user_a_id
      AND me.user_b = w.user_b_id
      AND me.ts = COALESCE(w.updated_at, w.created_at, NOW())
WHERE w.status = 'CONFIRMED'
  AND me.id IS NULL;

-- 4) Backfill message_events from message table.
INSERT INTO message_events (ts, sender_id, receiver_id, conversation_id)
SELECT
    COALESCE(m.date, NOW()) AS ts,
    m.user_from_id,
    m.user_to_id,
    m.conversation_id
FROM message m
LEFT JOIN message_events me
       ON me.sender_id = m.user_from_id
      AND me.receiver_id = m.user_to_id
      AND me.conversation_id = m.conversation_id
      AND me.ts = COALESCE(m.date, NOW())
WHERE me.id IS NULL;
