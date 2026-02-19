package com.nonononoki.alovoa.service.ml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class QdrantIntegrationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.aura.ml.qdrant.enabled:false}")
    private boolean enabled;

    @Value("${app.aura.ml.qdrant.url:http://localhost:6333}")
    private String qdrantUrl;

    @Value("${app.aura.ml.qdrant.api-key:}")
    private String apiKey;

    public QdrantIntegrationService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public IntegrationHealth health() {
        if (!enabled) {
            return new IntegrationHealth(false, "disabled");
        }

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    normalizedUrl() + "/collections",
                    HttpMethod.GET,
                    new HttpEntity<>(headers()),
                    String.class
            );
            if (response.getStatusCode().is2xxSuccessful()) {
                return new IntegrationHealth(true, "ok");
            }
            return new IntegrationHealth(false, "http_" + response.getStatusCode().value());
        } catch (Exception e) {
            return new IntegrationHealth(false, "error:" + e.getClass().getSimpleName());
        }
    }

    public Optional<QdrantAttractivenessHint> getAttractivenessHint(long userId,
                                                                     String segmentKey,
                                                                     String collection,
                                                                     double maxAbsDelta) {
        if (!enabled || userId <= 0 || collection == null || collection.isBlank()) {
            return Optional.empty();
        }

        double safeMaxAbs = clamp(maxAbsDelta, 0.0, 0.5);
        if (safeMaxAbs <= 0.0) {
            return Optional.empty();
        }

        JsonNode payload = loadPayload(collection, userId);
        if (payload == null) {
            return Optional.empty();
        }

        double boost = readDouble(payload.path("attractiveness_boost"), 0.0);
        JsonNode segmentBoosts = payload.path("segment_boosts");
        if (segmentKey != null && !segmentKey.isBlank() && segmentBoosts != null && segmentBoosts.isObject()) {
            boost += readDouble(segmentBoosts.path(segmentKey), 0.0);
        }

        if (!Double.isFinite(boost) || Math.abs(boost) <= 1e-9) {
            return Optional.empty();
        }

        String source = "qdrant:payload";
        JsonNode sourceModel = payload.path("source_model");
        if (sourceModel.isTextual() && !sourceModel.asText().isBlank()) {
            source = "qdrant:" + sourceModel.asText().trim();
        }

        return Optional.of(new QdrantAttractivenessHint(clamp(boost, -safeMaxAbs, safeMaxAbs), source));
    }

    public Optional<QdrantCandidateEnrichment> getCandidateEnrichment(long userId,
                                                                       String segmentKey,
                                                                       String collection,
                                                                       int limit) {
        if (!enabled || userId <= 0 || collection == null || collection.isBlank()) {
            return Optional.empty();
        }

        int safeLimit = Math.max(1, Math.min(100, limit));
        JsonNode payload = loadPayload(collection, userId);
        if (payload == null) {
            return Optional.empty();
        }

        LinkedHashSet<Long> candidateIds = new LinkedHashSet<>();
        if (segmentKey != null && !segmentKey.isBlank()) {
            JsonNode segmentBuckets = payload.path("segment_candidate_ids");
            if (segmentBuckets != null && segmentBuckets.isObject()) {
                pushCandidateIds(candidateIds, segmentBuckets.path(segmentKey), safeLimit);
            }
        }
        if (candidateIds.size() < safeLimit) {
            pushCandidateIds(candidateIds, payload.path("candidate_ids"), safeLimit);
        }

        if (candidateIds.isEmpty()) {
            return Optional.empty();
        }

        String source = "qdrant:payload";
        JsonNode sourceModel = payload.path("source_model");
        if (sourceModel.isTextual() && !sourceModel.asText().isBlank()) {
            source = "qdrant:" + sourceModel.asText().trim();
        }

        return Optional.of(new QdrantCandidateEnrichment(new ArrayList<>(candidateIds), source));
    }

    private JsonNode loadPayload(String collection, long userId) {
        try {
            String encodedCollection = URLEncoder.encode(collection, StandardCharsets.UTF_8);
            String encodedPoint = URLEncoder.encode("user:" + userId, StandardCharsets.UTF_8);
            String url = normalizedUrl()
                    + "/collections/" + encodedCollection
                    + "/points/" + encodedPoint
                    + "?with_payload=true&with_vector=false";
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers()),
                    String.class
            );
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return null;
            }
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode payload = root.path("result").path("payload");
            if (payload == null || !payload.isObject()) {
                return null;
            }
            return payload;
        } catch (Exception e) {
            return null;
        }
    }

    private void pushCandidateIds(Set<Long> out, JsonNode arrayNode, int limit) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return;
        }
        for (JsonNode node : arrayNode) {
            long id = node.asLong(-1);
            if (id <= 0) {
                continue;
            }
            out.add(id);
            if (out.size() >= limit) {
                return;
            }
        }
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        if (apiKey != null && !apiKey.isBlank()) {
            headers.set("api-key", apiKey);
        }
        return headers;
    }

    private String normalizedUrl() {
        if (qdrantUrl == null || qdrantUrl.isBlank()) {
            return "http://localhost:6333";
        }
        return qdrantUrl.endsWith("/") ? qdrantUrl.substring(0, qdrantUrl.length() - 1) : qdrantUrl;
    }

    private double readDouble(JsonNode node, double fallback) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return fallback;
        }
        if (node.isNumber()) {
            return node.asDouble(fallback);
        }
        try {
            return Double.parseDouble(node.asText());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) {
            return min;
        }
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}

