package com.nonononoki.alovoa.matching.rerank.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserStatsSnapshot {
    Long userId;
    String segmentKey;
    int impressions7d;
    int inboundLikes7d;
    int outboundLikes7d;
    int matches7d;
    int openMatches;
    int unreadThreads;
    int pendingInboundLikes;
    double a7d;
    double dPercentile7d;
    double backendAttractivenessScore;

    public static UserStatsSnapshot empty(Long userId, String segmentKey) {
        return UserStatsSnapshot.builder()
                .userId(userId)
                .segmentKey(segmentKey)
                .impressions7d(0)
                .inboundLikes7d(0)
                .outboundLikes7d(0)
                .matches7d(0)
                .openMatches(0)
                .unreadThreads(0)
                .pendingInboundLikes(0)
                .a7d(0.0)
                .dPercentile7d(0.5)
                .backendAttractivenessScore(0.5)
                .build();
    }
}
