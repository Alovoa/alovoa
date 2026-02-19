package com.nonononoki.alovoa.matching.rerank.policy.impl;

import com.nonononoki.alovoa.matching.rerank.model.RerankerConfig;
import com.nonononoki.alovoa.matching.rerank.model.UserStatsSnapshot;
import com.nonononoki.alovoa.matching.rerank.policy.CapacityPolicy;
import org.springframework.stereotype.Component;

@Component
public class DefaultCapacityPolicy implements CapacityPolicy {

    @Override
    public double factor(UserStatsSnapshot candidateStats, RerankerConfig config) {
        // C_j = open_matches + unread_conversations + pending_inbound_likes
        // f_capacity(j) = 1 / (1 + C_j / kappa)
        double c = Math.max(0, candidateStats.getOpenMatches())
                + Math.max(0, candidateStats.getUnreadThreads())
                + Math.max(0, candidateStats.getPendingInboundLikes());
        double kappa = Math.max(1e-6, config.getKappa());
        return 1.0 / (1.0 + (c / kappa));
    }
}
