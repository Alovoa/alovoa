package com.nonononoki.alovoa.matching.rerank.policy.impl;

import com.nonononoki.alovoa.matching.rerank.model.RerankerConfig;
import com.nonononoki.alovoa.matching.rerank.model.UserStatsSnapshot;
import com.nonononoki.alovoa.matching.rerank.policy.DesirabilityPolicy;
import org.springframework.stereotype.Component;

@Component
public class DefaultDesirabilityPolicy implements DesirabilityPolicy {

    @Override
    public double smoothedDesirability(UserStatsSnapshot stats, RerankerConfig config) {
        double rawPercentile = FormulaUtils.clamp01(stats.getDPercentile7d());
        double n = Math.max(0.0, stats.getImpressions7d());
        double baseline = FormulaUtils.clamp01(config.getBaselineAttractiveness());
        if (n <= 0.0) {
            return baseline;
        }
        double smoothedFromPercentile = FormulaUtils.clamp01(
                FormulaUtils.smooth(rawPercentile, n, baseline, config.getN0())
        );
        double backendPrior = FormulaUtils.clamp01(stats.getBackendAttractivenessScore());
        double w = FormulaUtils.smoothingWeight(n, config.getN0());
        return FormulaUtils.clamp01((w * smoothedFromPercentile) + ((1.0 - w) * backendPrior));
    }

    @Override
    public double gapFactor(UserStatsSnapshot viewerStats, UserStatsSnapshot candidateStats, RerankerConfig config) {
        // f_gap(i,j) = exp(-lambda * abs(D(i)-D(j)))
        double dViewer = smoothedDesirability(viewerStats, config);
        double dCandidate = smoothedDesirability(candidateStats, config);
        double lambda = Math.max(0.0, config.getLambda());
        return Math.exp(-lambda * Math.abs(dViewer - dCandidate));
    }

    @Override
    public int desirabilityDecile(UserStatsSnapshot stats, RerankerConfig config) {
        double d = smoothedDesirability(stats, config);
        int decile = (int) Math.floor(d * 10.0);
        if (decile >= 10) {
            return 9;
        }
        if (decile < 0) {
            return 0;
        }
        return decile;
    }
}
