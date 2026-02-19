package com.nonononoki.alovoa.matching.rerank.policy.impl;

import com.nonononoki.alovoa.matching.rerank.model.RerankerConfig;
import com.nonononoki.alovoa.matching.rerank.model.UserStatsSnapshot;
import com.nonononoki.alovoa.matching.rerank.policy.CollaborativePriorPolicy;
import org.springframework.stereotype.Component;

@Component
public class DefaultCollaborativePriorPolicy implements CollaborativePriorPolicy {

    @Override
    public double factor(UserStatsSnapshot viewerStats, UserStatsSnapshot candidateStats, RerankerConfig config) {
        if (viewerStats == null || candidateStats == null || config == null || !config.isEnableCollaborativePrior()) {
            return 1.0;
        }

        double viewerPrior = FormulaUtils.clamp01(viewerStats.getCollaborativePrior());
        double candidatePrior = FormulaUtils.clamp01(candidateStats.getCollaborativePrior());

        double alignment = 1.0 - Math.abs(viewerPrior - candidatePrior); // 0..1
        double candidateLift = candidatePrior - 0.5; // -0.5..0.5
        double centered = (0.70 * (alignment - 0.5)) + (0.30 * candidateLift);

        double confidence = FormulaUtils.clamp01(
                Math.max(viewerStats.getCollaborativeConfidence(), candidateStats.getCollaborativeConfidence())
        );
        double beta = Math.max(0.0, config.getCollaborativeBeta());
        double rawFactor = 1.0 + (beta * confidence * (2.0 * centered));

        double minFactor = Math.max(0.0, config.getCollaborativeMinFactor());
        double maxFactor = Math.max(minFactor, config.getCollaborativeMaxFactor());
        if (rawFactor < minFactor) {
            return minFactor;
        }
        if (rawFactor > maxFactor) {
            return maxFactor;
        }
        return rawFactor;
    }
}

