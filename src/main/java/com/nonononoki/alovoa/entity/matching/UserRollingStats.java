package com.nonononoki.alovoa.entity.matching;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "user_rolling_stats", uniqueConstraints = {
        @UniqueConstraint(name = "uk_rolling_stats_user_segment", columnNames = {"user_id", "segment_key"})
})
public class UserRollingStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "segment_key", nullable = false, length = 255)
    private String segmentKey;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "window_start", nullable = false)
    private Date windowStart;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "window_end", nullable = false)
    private Date windowEnd;

    @Column(name = "impressions_7d", nullable = false)
    private int impressions7d;

    @Column(name = "inbound_likes_7d", nullable = false)
    private int inboundLikes7d;

    @Column(name = "outbound_likes_7d", nullable = false)
    private int outboundLikes7d;

    @Column(name = "matches_7d", nullable = false)
    private int matches7d;

    @Column(name = "open_matches", nullable = false)
    private int openMatches;

    @Column(name = "unread_threads", nullable = false)
    private int unreadThreads;

    @Column(name = "pending_inbound_likes", nullable = false)
    private int pendingInboundLikes;

    @Column(name = "A_7d", nullable = false)
    private double a7d;

    @Column(name = "D_percentile_7d", nullable = false)
    private double dPercentile7d;

    @Column(name = "backend_attractiveness_score", nullable = false)
    private double backendAttractivenessScore;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Date updatedAt;
}
