package com.nonononoki.alovoa.matching.rerank.policy;

import com.nonononoki.alovoa.matching.rerank.model.RerankerConfig;
import com.nonononoki.alovoa.matching.rerank.model.UserStatsSnapshot;

public interface DesirabilityPolicy {
    double smoothedDesirability(UserStatsSnapshot stats, RerankerConfig config);
    double gapFactor(UserStatsSnapshot viewerStats, UserStatsSnapshot candidateStats, RerankerConfig config);
    int desirabilityDecile(UserStatsSnapshot stats, RerankerConfig config);
}
