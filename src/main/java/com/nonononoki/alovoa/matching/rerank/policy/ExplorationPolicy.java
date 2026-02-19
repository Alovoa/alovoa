package com.nonononoki.alovoa.matching.rerank.policy;

import com.nonononoki.alovoa.matching.rerank.model.RerankerConfig;
import com.nonononoki.alovoa.matching.rerank.model.UserStatsSnapshot;

import java.util.Map;

public interface ExplorationPolicy {
    double ucbBonus(UserStatsSnapshot viewerStats,
                    UserStatsSnapshot candidateStats,
                    int candidateDecile,
                    Map<Integer, Integer> viewerBucketImpressions,
                    RerankerConfig config);
}
