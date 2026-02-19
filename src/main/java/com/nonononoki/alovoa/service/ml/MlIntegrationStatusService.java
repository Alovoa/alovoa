package com.nonononoki.alovoa.service.ml;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MlIntegrationStatusService {

    private final QdrantIntegrationService qdrantIntegrationService;
    private final UnleashIntegrationService unleashIntegrationService;
    private final OpenFgaIntegrationService openFgaIntegrationService;

    @Value("${app.aura.ml.qdrant.candidate-enrichment.enabled:false}")
    private boolean qdrantCandidateEnrichmentEnabled;

    @Value("${app.aura.ml.qdrant.candidate-enrichment.collection:candidate_enrichment}")
    private String qdrantCandidateCollection;

    @Value("${app.aura.ml.qdrant.candidate-enrichment.limit:20}")
    private int qdrantCandidateLimit;

    @Value("${app.aura.ml.qdrant.attractiveness-hint.enabled:false}")
    private boolean qdrantAttractivenessHintEnabled;

    @Value("${app.aura.ml.qdrant.attractiveness-hint.collection:attractiveness_hints}")
    private String qdrantAttractivenessCollection;

    @Value("${app.aura.ml.qdrant.attractiveness-hint.max-delta:0.08}")
    private double qdrantAttractivenessHintMaxDelta;

    public MlIntegrationStatusService(QdrantIntegrationService qdrantIntegrationService,
                                      UnleashIntegrationService unleashIntegrationService,
                                      OpenFgaIntegrationService openFgaIntegrationService) {
        this.qdrantIntegrationService = qdrantIntegrationService;
        this.unleashIntegrationService = unleashIntegrationService;
        this.openFgaIntegrationService = openFgaIntegrationService;
    }

    public Map<String, Object> status() {
        IntegrationHealth qdrant = qdrantIntegrationService.health();
        IntegrationHealth unleash = unleashIntegrationService.health();
        IntegrationHealth openfga = openFgaIntegrationService.health();

        Map<String, Object> flags = new LinkedHashMap<>();
        flags.put("qdrantEnabled", qdrantIntegrationService.isEnabled());
        flags.put("unleashEnabled", unleashIntegrationService.isEnabled());
        flags.put("openfgaEnabled", openFgaIntegrationService.isEnabled());
        flags.put("qdrantCandidateEnrichmentEnabled", qdrantCandidateEnrichmentEnabled);
        flags.put("qdrantAttractivenessHintEnabled", qdrantAttractivenessHintEnabled);
        flags.put("qdrantCandidateCollection", qdrantCandidateCollection);
        flags.put("qdrantAttractivenessCollection", qdrantAttractivenessCollection);
        flags.put("qdrantAttractivenessHintMaxDelta", qdrantAttractivenessHintMaxDelta);
        flags.put("qdrantCandidateLimit", qdrantCandidateLimit);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("qdrant", Map.of("ok", qdrant.ok(), "message", qdrant.message()));
        response.put("unleash", Map.of("ok", unleash.ok(), "message", unleash.message()));
        response.put("openfga", Map.of("ok", openfga.ok(), "message", openfga.message()));
        response.put("flags", flags);
        return response;
    }

    public Map<String, Object> candidateEnrichment(long userId, String segmentKey, Integer requestedLimit) {
        String safeSegment = (segmentKey == null || segmentKey.isBlank()) ? "default" : segmentKey;
        int limit = requestedLimit == null ? qdrantCandidateLimit : requestedLimit;
        limit = Math.max(1, Math.min(100, limit));

        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("user_id", userId);
        fallback.put("segment_key", safeSegment);
        fallback.put("source", "disabled");
        fallback.put("candidate_ids", List.of());

        if (!qdrantCandidateEnrichmentEnabled || !qdrantIntegrationService.isEnabled()) {
            return fallback;
        }

        return qdrantIntegrationService
                .getCandidateEnrichment(userId, segmentKey, qdrantCandidateCollection, limit)
                .<Map<String, Object>>map(result -> {
                    Map<String, Object> response = new LinkedHashMap<>();
                    response.put("user_id", userId);
                    response.put("segment_key", safeSegment);
                    response.put("source", result.source());
                    response.put("candidate_ids", result.candidateIds());
                    return response;
                })
                .orElse(fallback);
    }
}

