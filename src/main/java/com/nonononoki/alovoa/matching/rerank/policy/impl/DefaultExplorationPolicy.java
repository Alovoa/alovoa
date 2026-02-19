package com.nonononoki.alovoa.matching.rerank.policy.impl;

import com.nonononoki.alovoa.matching.rerank.model.RerankerConfig;
import com.nonononoki.alovoa.matching.rerank.model.UserStatsSnapshot;
import com.nonononoki.alovoa.matching.rerank.policy.ExplorationPolicy;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DefaultExplorationPolicy implements ExplorationPolicy {

    @Override
    public double ucbBonus(UserStatsSnapshot viewerStats,
                           UserStatsSnapshot candidateStats,
                           int candidateDecile,
                           Map<Integer, Integer> viewerBucketImpressions,
                           RerankerConfig config) {
        if (!config.isEnableExploration() || config.getEpsilon() <= 0.0) {
            return 0.0;
        }

        int totalSeen = Math.max(0, viewerStats.getImpressions7d());
        int seenForBucket = Math.max(0, viewerBucketImpressions.getOrDefault(candidateDecile, 0));

        // UCB-style exploration: inverse of exposure for j-like bucket, adjusted by candidate underexposure.
        double ucb = Math.sqrt((2.0 * Math.log(1.0 + totalSeen + 1.0)) / (1.0 + seenForBucket));
        double candidateUnderExposure = 1.0 / Math.sqrt(1.0 + Math.max(0.0, candidateStats.getImpressions7d()));
        return ucb * candidateUnderExposure;
    }
}
