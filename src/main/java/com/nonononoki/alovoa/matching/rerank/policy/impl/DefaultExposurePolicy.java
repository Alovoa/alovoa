package com.nonononoki.alovoa.matching.rerank.policy.impl;

import com.nonononoki.alovoa.matching.rerank.model.RerankerConfig;
import com.nonononoki.alovoa.matching.rerank.model.UserStatsSnapshot;
import com.nonononoki.alovoa.matching.rerank.policy.ExposurePolicy;
import org.springframework.stereotype.Component;

@Component
public class DefaultExposurePolicy implements ExposurePolicy {

    @Override
    public double factor(UserStatsSnapshot candidateStats, RerankerConfig config) {
        // f_exposure(j) = 1 / (1 + E_j / tau)^p
        double exposure = Math.max(0, candidateStats.getImpressions7d());
        double tau = Math.max(1e-6, config.getTau());
        double p = Math.max(0.0, config.getP());
        return 1.0 / Math.pow(1.0 + (exposure / tau), p);
    }
}
