package com.nonononoki.alovoa.matching.rerank.policy;

import com.nonononoki.alovoa.matching.rerank.model.RerankerConfig;
import com.nonononoki.alovoa.matching.rerank.model.UserStatsSnapshot;

public interface ExposurePolicy {
    double factor(UserStatsSnapshot candidateStats, RerankerConfig config);
}
