package com.nonononoki.alovoa.matching.rerank.service;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.matching.rerank.model.*;
import com.nonononoki.alovoa.matching.rerank.policy.CapacityPolicy;
import com.nonononoki.alovoa.matching.rerank.policy.DesirabilityPolicy;
import com.nonononoki.alovoa.matching.rerank.policy.ExplorationPolicy;
import com.nonononoki.alovoa.matching.rerank.policy.ExposurePolicy;
import com.nonononoki.alovoa.model.MatchRecommendationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MatchRerankerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MatchRerankerService.class);

    private final CandidateGenerator candidateGenerator;
    private final SegmentKeyService segmentKeyService;
    private final RerankerPolicyConfigService policyConfigService;
    private final RollingStatsReadService rollingStatsReadService;
    private final ExposurePolicy exposurePolicy;
    private final CapacityPolicy capacityPolicy;
    private final DesirabilityPolicy desirabilityPolicy;
    private final ExplorationPolicy explorationPolicy;

    public MatchRerankerService(CandidateGenerator candidateGenerator,
                                SegmentKeyService segmentKeyService,
                                RerankerPolicyConfigService policyConfigService,
                                RollingStatsReadService rollingStatsReadService,
                                ExposurePolicy exposurePolicy,
                                CapacityPolicy capacityPolicy,
                                DesirabilityPolicy desirabilityPolicy,
                                ExplorationPolicy explorationPolicy) {
        this.candidateGenerator = candidateGenerator;
        this.segmentKeyService = segmentKeyService;
        this.policyConfigService = policyConfigService;
        this.rollingStatsReadService = rollingStatsReadService;
        this.exposurePolicy = exposurePolicy;
        this.capacityPolicy = capacityPolicy;
        this.desirabilityPolicy = desirabilityPolicy;
        this.explorationPolicy = explorationPolicy;
    }

    public RerankResult rerank(User viewer,
                               List<MatchRecommendationDto> existingCandidates,
                               String requestId,
                               String surface) {
        if (viewer == null || existingCandidates == null || existingCandidates.isEmpty()) {
            return RerankResult.builder()
                    .ranked(existingCandidates == null ? List.of() : existingCandidates)
                    .scoreTraces(Map.of())
                    .rerankerApplied(false)
                    .reason("empty_input")
                    .segmentKey("unknown")
                    .variant("control")
                    .build();
        }

        String segmentKey = segmentKeyService.segmentKey(viewer);
        RerankerPolicyConfigService.ResolvedConfig resolvedConfig = policyConfigService.resolve(segmentKey);
        String variant = policyConfigService.assignVariant(viewer.getId(), resolvedConfig);

        if (!resolvedConfig.isEnabled() || !"treatment".equals(variant)) {
            return RerankResult.builder()
                    .ranked(existingCandidates)
                    .scoreTraces(Map.of())
                    .rerankerApplied(false)
                    .reason("flag_disabled_or_control")
                    .segmentKey(segmentKey)
                    .variant(variant)
                    .build();
        }

        RerankerConfig config = resolvedConfig.getConfig();
        MatchingRequestContext context = MatchingRequestContext.builder()
                .requestId(requestId)
                .surface(surface == null || surface.isBlank() ? "daily_matches" : surface)
                .segmentKey(segmentKey)
                .variant(variant)
                .build();

        try {
            List<RerankCandidate> candidates = candidateGenerator.getCandidates(viewer, existingCandidates, context);

            // Hard quality floor: keep only candidates with sufficient base quality.
            List<RerankCandidate> filtered = candidates.stream()
                    .filter(c -> c.getBaseScore() >= config.getSMin())
                    .collect(Collectors.toList());

            if (filtered.isEmpty()) {
                return RerankResult.builder()
                        .ranked(List.of())
                        .scoreTraces(Map.of())
                        .rerankerApplied(true)
                        .reason("all_candidates_below_s_min")
                        .segmentKey(segmentKey)
                        .variant(variant)
                        .build();
            }

            List<Long> candidateIds = filtered.stream().map(RerankCandidate::getCandidateId).toList();
            UserStatsSnapshot viewerStats = rollingStatsReadService.getSnapshot(viewer.getId(), segmentKey);
            Map<Long, UserStatsSnapshot> candidateStatsMap = rollingStatsReadService.getSnapshots(candidateIds, segmentKey);
            Map<Integer, Integer> viewerBucketImpressions = rollingStatsReadService.getViewerBucketImpressions(viewer.getId());

            List<RerankedCandidate> scored = new ArrayList<>();
            Map<Long, ScoreTrace> traces = new HashMap<>();

            for (RerankCandidate candidate : filtered) {
                UserStatsSnapshot candidateStats = candidateStatsMap.getOrDefault(
                        candidate.getCandidateId(),
                        UserStatsSnapshot.empty(candidate.getCandidateId(), segmentKey)
                );

                double s = candidate.getBaseScore();
                double fExposure = exposurePolicy.factor(candidateStats, config);
                double fCapacity = capacityPolicy.factor(candidateStats, config);
                double fGap = desirabilityPolicy.gapFactor(viewerStats, candidateStats, config);
                int decile = desirabilityPolicy.desirabilityDecile(candidateStats, config);
                double ucb = explorationPolicy.ucbBonus(
                        viewerStats,
                        candidateStats,
                        decile,
                        viewerBucketImpressions,
                        config
                );

                double finalScore = (s * fExposure * fCapacity * fGap) + (config.getEpsilon() * ucb);

                Map<String, Object> windowStats = Map.of(
                        "E_7d", candidateStats.getImpressions7d(),
                        "C_openMatches", candidateStats.getOpenMatches(),
                        "C_unreadThreads", candidateStats.getUnreadThreads(),
                        "C_pendingInboundLikes", candidateStats.getPendingInboundLikes(),
                        "D_viewer", desirabilityPolicy.smoothedDesirability(viewerStats, config),
                        "D_candidate", desirabilityPolicy.smoothedDesirability(candidateStats, config),
                        "backendAttractiveness_viewer", viewerStats.getBackendAttractivenessScore(),
                        "backendAttractiveness_candidate", candidateStats.getBackendAttractivenessScore()
                );

                ScoreTrace trace = ScoreTrace.builder()
                        .s(s)
                        .fExposure(fExposure)
                        .fCapacity(fCapacity)
                        .fGap(fGap)
                        .ucb(ucb)
                        .finalScore(finalScore)
                        .segment(segmentKey)
                        .desirabilityDecile(decile)
                        .windowStats(windowStats)
                        .build();

                traces.put(candidate.getCandidateId(), trace);
                scored.add(RerankedCandidate.builder()
                        .recommendation(candidate.getPayload())
                        .scoreTrace(trace)
                        .originalPosition(candidate.getOriginalPosition())
                        .build());
            }

            scored.sort(Comparator
                    .comparing((RerankedCandidate c) -> c.getScoreTrace().getFinalScore()).reversed()
                    .thenComparing(c -> nvl(c.getRecommendation().getCompatibilityScore()), Comparator.reverseOrder())
                    .thenComparing(RerankedCandidate::getOriginalPosition)
                    .thenComparing(c -> nvlLong(c.getRecommendation().getUserId())));

            List<MatchRecommendationDto> ranked = scored.stream()
                    .map(RerankedCandidate::getRecommendation)
                    .collect(Collectors.toList());

            return RerankResult.builder()
                    .ranked(ranked)
                    .scoreTraces(traces)
                    .rerankerApplied(true)
                    .reason("ok")
                    .segmentKey(segmentKey)
                    .variant(variant)
                    .build();

        } catch (Exception e) {
            LOGGER.error("Reranker failed. Falling back to existing S ordering.", e);
            return RerankResult.builder()
                    .ranked(existingCandidates)
                    .scoreTraces(Map.of())
                    .rerankerApplied(false)
                    .reason("reranker_error_fallback")
                    .segmentKey(segmentKey)
                    .variant("control")
                    .build();
        }
    }

    private Double nvl(Double value) {
        return value == null ? 0.0 : value;
    }

    private Long nvlLong(Long value) {
        return value == null ? Long.MAX_VALUE : value;
    }
}
