package com.nonononoki.alovoa.matching.rerank.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.entity.matching.FeatureFlagConfig;
import com.nonononoki.alovoa.matching.rerank.model.RerankerConfig;
import com.nonononoki.alovoa.repo.matching.FeatureFlagConfigRepository;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RerankerPolicyConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RerankerPolicyConfigService.class);
    public static final String FLAG_NAME = "MATCH_RERANKER";

    private final FeatureFlagConfigRepository featureFlagRepo;
    private final ObjectMapper objectMapper;

    public RerankerPolicyConfigService(FeatureFlagConfigRepository featureFlagRepo,
                                       ObjectMapper objectMapper) {
        this.featureFlagRepo = featureFlagRepo;
        this.objectMapper = objectMapper;
    }

    public ResolvedConfig resolve(String segmentKey) {
        Optional<FeatureFlagConfig> segmentConfig = featureFlagRepo.findByFlagNameAndSegmentKey(FLAG_NAME, segmentKey);
        if (segmentConfig.isPresent()) {
            return fromEntity(segmentConfig.get(), "segment");
        }

        return featureFlagRepo.findByFlagNameAndSegmentKey(FLAG_NAME, "*")
                .map(ff -> fromEntity(ff, "global"))
                .orElseGet(() -> ResolvedConfig.builder()
                        .enabled(false)
                        .config(new RerankerConfig())
                        .source("default")
                        .segmentKey(segmentKey)
                        .build());
    }

    public String assignVariant(Long userId, ResolvedConfig resolvedConfig) {
        if (userId == null || !resolvedConfig.isEnabled()) {
            return "control";
        }

        RerankerConfig config = resolvedConfig.getConfig();
        int trafficPercent = Math.max(0, Math.min(100, config.getTrafficPercent()));
        if (trafficPercent <= 0) {
            return "control";
        }
        if (trafficPercent >= 100) {
            return "treatment";
        }

        int bucket = Math.floorMod((FLAG_NAME + ":" + userId).hashCode(), 100);
        return bucket < trafficPercent ? "treatment" : "control";
    }

    private ResolvedConfig fromEntity(FeatureFlagConfig featureFlag, String source) {
        RerankerConfig parsed = new RerankerConfig();
        if (featureFlag.getJsonConfig() != null && !featureFlag.getJsonConfig().isBlank()) {
            try {
                RerankerConfig cfg = objectMapper.readValue(featureFlag.getJsonConfig(), RerankerConfig.class);
                if (cfg != null) {
                    parsed = cfg;
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to parse reranker config JSON for segment {}. Using defaults.",
                        featureFlag.getSegmentKey(), e);
            }
        }

        return ResolvedConfig.builder()
                .enabled(featureFlag.isEnabled())
                .segmentKey(featureFlag.getSegmentKey())
                .source(source)
                .config(parsed)
                .build();
    }

    @Value
    @Builder
    @ToString(onlyExplicitlyIncluded = true)
    public static class ResolvedConfig {
        @ToString.Include
        boolean enabled;
        @ToString.Include
        String segmentKey;
        @ToString.Include
        String source;
        RerankerConfig config;
    }
}
