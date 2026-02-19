package com.nonononoki.alovoa.matching.rerank.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.FaceQualityEvent;
import com.nonononoki.alovoa.repo.FaceQualityEventRepository;
import com.nonononoki.alovoa.service.ml.JavaMediaBackendService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class FaceQualityScoringService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FaceQualityScoringService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final FaceQualityEventRepository faceQualityEventRepo;
    private final JavaMediaBackendService javaMediaBackendService;

    @Value("${app.aura.media-service.url:http://localhost:8001}")
    private String mediaServiceUrl;

    @Value("${app.aura.face-quality.enabled:true}")
    private boolean enabled;

    public FaceQualityScoringService(RestTemplate restTemplate,
                                     ObjectMapper objectMapper,
                                     FaceQualityEventRepository faceQualityEventRepo,
                                     JavaMediaBackendService javaMediaBackendService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.faceQualityEventRepo = faceQualityEventRepo;
        this.javaMediaBackendService = javaMediaBackendService;
    }

    public Optional<FaceQualityAssessment> assessAndLog(User user,
                                                        String contentType,
                                                        String imageBase64,
                                                        String segmentKey) {
        if (!enabled || user == null || user.getId() == null || imageBase64 == null || imageBase64.isBlank()) {
            return Optional.empty();
        }

        try {
            JsonNode root;
            if (javaMediaBackendService.isEnabled()) {
                byte[] bytes = decodeBase64Image(imageBase64);
                Map<String, Object> payload = javaMediaBackendService.scoreFaceQuality(bytes);
                root = objectMapper.valueToTree(payload);
            } else {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                Map<String, Object> body = new HashMap<>();
                body.put("user_id", user.getId());
                body.put("image_base64", imageBase64);
                if (contentType != null && !contentType.isBlank()) {
                    body.put("surface", contentType);
                }
                if (segmentKey != null && !segmentKey.isBlank()) {
                    body.put("segment_key", segmentKey);
                }

                ResponseEntity<String> response = restTemplate.exchange(
                        mediaServiceUrl + "/quality/face",
                        HttpMethod.POST,
                        new HttpEntity<>(body, headers),
                        String.class
                );
                if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                    return Optional.empty();
                }
                root = objectMapper.readTree(response.getBody());
            }
            double score = clamp01(root.path("quality_score").asDouble(0.5));
            double confidence = clamp01(root.path("confidence").asDouble(0.0));
            String provider = root.path("provider").asText("face_quality");
            String modelVersion = root.path("model_version").isTextual() ? root.path("model_version").asText() : null;

            String signalJson = null;
            JsonNode signals = root.path("signals");
            if (signals != null && !signals.isMissingNode()) {
                try {
                    signalJson = objectMapper.writeValueAsString(signals);
                } catch (Exception ignored) {
                    signalJson = null;
                }
            }

            FaceQualityEvent event = new FaceQualityEvent();
            event.setUser(user);
            event.setContentType(contentType);
            event.setQualityScore(score);
            event.setConfidence(confidence);
            event.setProvider(provider);
            event.setModelVersion(modelVersion);
            event.setSignalJson(signalJson);
            event.setCreatedAt(new Date());
            faceQualityEventRepo.save(event);

            return Optional.of(new FaceQualityAssessment(score, confidence, provider, modelVersion, signalJson));
        } catch (Exception e) {
            LOGGER.debug("Face quality scoring failed for user {}", user.getId(), e);
            return Optional.empty();
        }
    }

    private double clamp01(double value) {
        if (!Double.isFinite(value)) {
            return 0.0;
        }
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }

    private byte[] decodeBase64Image(String imageBase64) {
        if (imageBase64 == null || imageBase64.isBlank()) {
            return new byte[0];
        }
        String payload = imageBase64;
        int idx = payload.indexOf(',');
        if (idx >= 0 && idx < payload.length() - 1) {
            payload = payload.substring(idx + 1);
        }
        try {
            return Base64.getDecoder().decode(payload);
        } catch (Exception ignored) {
            return new byte[0];
        }
    }

    public record FaceQualityAssessment(
            double qualityScore,
            double confidence,
            String provider,
            String modelVersion,
            String signalJson
    ) {
    }
}
