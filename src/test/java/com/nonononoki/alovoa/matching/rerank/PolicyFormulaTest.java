package com.nonononoki.alovoa.matching.rerank;

import com.nonononoki.alovoa.matching.rerank.model.RerankerConfig;
import com.nonononoki.alovoa.matching.rerank.model.UserStatsSnapshot;
import com.nonononoki.alovoa.matching.rerank.policy.impl.DefaultCapacityPolicy;
import com.nonononoki.alovoa.matching.rerank.policy.impl.DefaultDesirabilityPolicy;
import com.nonononoki.alovoa.matching.rerank.policy.impl.DefaultExplorationPolicy;
import com.nonononoki.alovoa.matching.rerank.policy.impl.DefaultExposurePolicy;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyFormulaTest {

    private final DefaultExposurePolicy exposurePolicy = new DefaultExposurePolicy();
    private final DefaultCapacityPolicy capacityPolicy = new DefaultCapacityPolicy();
    private final DefaultDesirabilityPolicy desirabilityPolicy = new DefaultDesirabilityPolicy();
    private final DefaultExplorationPolicy explorationPolicy = new DefaultExplorationPolicy();

    @Test
    void exposureFactorMatchesFormula() {
        RerankerConfig cfg = new RerankerConfig();
        cfg.setTau(200.0);
        cfg.setP(1.0);

        UserStatsSnapshot candidate = UserStatsSnapshot.builder()
                .userId(1L)
                .segmentKey("seg")
                .impressions7d(200)
                .build();

        double factor = exposurePolicy.factor(candidate, cfg);
        assertEquals(0.5, factor, 1e-9);
    }

    @Test
    void capacityFactorMatchesFormula() {
        RerankerConfig cfg = new RerankerConfig();
        cfg.setKappa(30.0);

        UserStatsSnapshot candidate = UserStatsSnapshot.builder()
                .userId(1L)
                .segmentKey("seg")
                .openMatches(10)
                .unreadThreads(10)
                .pendingInboundLikes(10)
                .build();

        double factor = capacityPolicy.factor(candidate, cfg);
        assertEquals(0.5, factor, 1e-9);
    }

    @Test
    void coldStartSmoothingFallsBackToBaseline() {
        RerankerConfig cfg = new RerankerConfig();
        cfg.setN0(200.0);
        cfg.setBaselineAttractiveness(0.5);

        UserStatsSnapshot noEvidence = UserStatsSnapshot.builder()
                .userId(1L)
                .segmentKey("seg")
                .impressions7d(0)
                .dPercentile7d(0.95)
                .build();

        assertEquals(0.5, desirabilityPolicy.smoothedDesirability(noEvidence, cfg), 1e-9);
    }

    @Test
    void desirabilityGapUsesExponentialPenalty() {
        RerankerConfig cfg = new RerankerConfig();
        cfg.setLambda(2.0);
        cfg.setN0(1.0);

        UserStatsSnapshot viewer = UserStatsSnapshot.builder()
                .userId(1L)
                .segmentKey("seg")
                .impressions7d(1000)
                .dPercentile7d(0.2)
                .backendAttractivenessScore(0.2)
                .build();

        UserStatsSnapshot candidate = UserStatsSnapshot.builder()
                .userId(2L)
                .segmentKey("seg")
                .impressions7d(1000)
                .dPercentile7d(0.8)
                .backendAttractivenessScore(0.8)
                .build();

        double expected = Math.exp(-2.0 * Math.abs(0.2 - 0.8));
        assertEquals(expected, desirabilityPolicy.gapFactor(viewer, candidate, cfg), 1e-3);
    }

    @Test
    void explorationBonusIsPositiveForUnderExposedBucket() {
        RerankerConfig cfg = new RerankerConfig();
        cfg.setEpsilon(0.01);
        cfg.setEnableExploration(true);

        UserStatsSnapshot viewer = UserStatsSnapshot.builder()
                .userId(1L)
                .segmentKey("seg")
                .impressions7d(120)
                .build();

        UserStatsSnapshot candidate = UserStatsSnapshot.builder()
                .userId(2L)
                .segmentKey("seg")
                .impressions7d(5)
                .build();

        double bonus = explorationPolicy.ucbBonus(viewer, candidate, 7, Map.of(7, 1), cfg);
        assertTrue(bonus > 0.0);
    }
}
