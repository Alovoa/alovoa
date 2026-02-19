package com.nonononoki.alovoa.matching.rerank.service;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.matching.UserVisualAttractiveness;
import com.nonononoki.alovoa.entity.user.UserProfilePicture;
import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.repo.matching.UserVisualAttractivenessRepository;
import com.nonononoki.alovoa.service.S3StorageService;
import com.nonononoki.alovoa.service.ml.JavaMediaBackendService;
import com.nonononoki.alovoa.service.ml.OpenFgaIntegrationService;
import com.nonononoki.alovoa.service.ml.QdrantAttractivenessHint;
import com.nonononoki.alovoa.service.ml.QdrantIntegrationService;
import com.nonononoki.alovoa.service.ml.UnleashIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(value = "app.aura.attractiveness.enabled", havingValue = "true", matchIfMissing = true)
public class VisualAttractivenessSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VisualAttractivenessSyncService.class);
    private static final String DEFAULT_PROVIDER = "deepface+insightface+mediapipe";
    private static final String DEFAULT_MODEL_VERSION = "oss_v1";

    private final UserRepository userRepo;
    private final UserVisualAttractivenessRepository visualRepo;
    private final S3StorageService s3StorageService;
    private final RestTemplate restTemplate;
    private final FaceQualityScoringService faceQualityScoringService;
    private final SegmentKeyService segmentKeyService;
    private final QdrantIntegrationService qdrantIntegrationService;
    private final UnleashIntegrationService unleashIntegrationService;
    private final OpenFgaIntegrationService openFgaIntegrationService;
    private final JavaMediaBackendService javaMediaBackendService;

    @Value("${app.aura.media-service.url:http://localhost:8001}")
    private String mediaServiceUrl;

    @Value("${app.aura.attractiveness.max-users-per-run:500}")
    private int maxUsersPerRun;

    @Value("${app.aura.attractiveness.rescore-days:14}")
    private int rescoreDays;

    @Value("${app.aura.face-quality.enabled:true}")
    private boolean faceQualityEnabled;

    @Value("${app.aura.attractiveness.quality-gate.enabled:true}")
    private boolean qualityGateEnabled;

    @Value("${app.aura.attractiveness.quality-gate.min-score:0.30}")
    private double qualityGateMinScore;

    @Value("${app.aura.attractiveness.quality-gate.min-confidence:0.20}")
    private double qualityGateMinConfidence;

    @Value("${app.aura.attractiveness.quality-blend-weight:0.20}")
    private double qualityBlendWeight;

    @Value("${app.aura.attractiveness.use-unleash-flag:false}")
    private boolean useUnleashFlag;

    @Value("${app.aura.attractiveness.unleash-flag-name:attractiveness_local_enabled}")
    private String unleashFlagName;

    @Value("${app.aura.attractiveness.openfga-enforce:false}")
    private boolean openfgaEnforce;

    @Value("${app.aura.attractiveness.openfga-object:feature:attractiveness_local}")
    private String openfgaObject;

    @Value("${app.aura.attractiveness.openfga-relation:can_use}")
    private String openfgaRelation;

    @Value("${app.aura.ml.qdrant.attractiveness-hint.enabled:false}")
    private boolean qdrantAttractivenessHintEnabled;

    @Value("${app.aura.ml.qdrant.attractiveness-hint.collection:attractiveness_hints}")
    private String qdrantAttractivenessCollection;

    @Value("${app.aura.ml.qdrant.attractiveness-hint.max-delta:0.08}")
    private double qdrantAttractivenessHintMaxDelta;

    public VisualAttractivenessSyncService(UserRepository userRepo,
                                           UserVisualAttractivenessRepository visualRepo,
                                           S3StorageService s3StorageService,
                                           RestTemplate restTemplate,
                                           FaceQualityScoringService faceQualityScoringService,
                                           SegmentKeyService segmentKeyService,
                                           QdrantIntegrationService qdrantIntegrationService,
                                           UnleashIntegrationService unleashIntegrationService,
                                           OpenFgaIntegrationService openFgaIntegrationService,
                                           JavaMediaBackendService javaMediaBackendService) {
        this.userRepo = userRepo;
        this.visualRepo = visualRepo;
        this.s3StorageService = s3StorageService;
        this.restTemplate = restTemplate;
        this.faceQualityScoringService = faceQualityScoringService;
        this.segmentKeyService = segmentKeyService;
        this.qdrantIntegrationService = qdrantIntegrationService;
        this.unleashIntegrationService = unleashIntegrationService;
        this.openFgaIntegrationService = openFgaIntegrationService;
        this.javaMediaBackendService = javaMediaBackendService;
    }

    @Scheduled(cron = "${app.aura.attractiveness.cron:0 45 2 * * *}")
    public void scheduledRefresh() {
        refreshVisualScores();
    }

    public void refreshVisualScores() {
        List<User> users = userRepo.findByDisabledFalseAndAdminFalseAndConfirmedTrue();
        if (users.isEmpty()) {
            return;
        }

        List<User> eligible = users.stream()
                .filter(u -> u.getProfilePicture() != null)
                .filter(u -> u.getProfilePicture().getS3Key() != null && !u.getProfilePicture().getS3Key().isBlank())
                .sorted(Comparator.comparing(User::getId))
                .collect(Collectors.toList());

        if (eligible.isEmpty()) {
            return;
        }

        List<Long> eligibleIds = eligible.stream().map(User::getId).toList();
        Map<Long, UserVisualAttractiveness> existing = visualRepo.findByUserIdIn(eligibleIds).stream()
                .collect(Collectors.toMap(UserVisualAttractiveness::getUserId, Function.identity(), (a, b) -> a));

        Instant refreshCutoff = Instant.now().minus(Math.max(1, rescoreDays), ChronoUnit.DAYS);
        int processed = 0;
        int skippedFresh = 0;
        int skippedNoMedia = 0;
        int skippedPolicy = 0;
        int skippedQuality = 0;
        int failed = 0;

        boolean enforceUnleash = useUnleashFlag
                && unleashIntegrationService.isEnabled()
                && unleashIntegrationService.health().ok();
        boolean enforceOpenfga = openfgaEnforce
                && openFgaIntegrationService.isEnabled()
                && openFgaIntegrationService.health().ok();

        for (User user : eligible) {
            if (processed >= Math.max(1, maxUsersPerRun)) {
                break;
            }

            UserVisualAttractiveness current = existing.get(user.getId());
            if (isFresh(current, refreshCutoff)) {
                skippedFresh++;
                continue;
            }

            String frontImageBase64 = loadProfilePictureAsBase64(user).orElse(null);
            if (frontImageBase64 == null) {
                skippedNoMedia++;
                continue;
            }

            try {
                String segmentKey = segmentKeyService.segmentKey(user);

                if (enforceUnleash && !isAttractivenessEnabledByUnleash(user, segmentKey)) {
                    skippedPolicy++;
                    continue;
                }
                if (enforceOpenfga && !openFgaIntegrationService.checkAccess("user:" + user.getId(), openfgaRelation, openfgaObject)) {
                    skippedPolicy++;
                    continue;
                }

                Optional<FaceQualityScoringService.FaceQualityAssessment> qualityAssessment = Optional.empty();
                if (faceQualityEnabled) {
                    qualityAssessment = faceQualityScoringService.assessAndLog(
                            user,
                            "PROFILE_PICTURE",
                            frontImageBase64,
                            segmentKey
                    );
                }

                if (qualityGateEnabled && qualityAssessment.isPresent()) {
                    FaceQualityScoringService.FaceQualityAssessment assessment = qualityAssessment.get();
                    if (assessment.qualityScore() < clamp01(qualityGateMinScore)
                            || assessment.confidence() < clamp01(qualityGateMinConfidence)) {
                        skippedQuality++;
                        continue;
                    }
                }

                Map<String, Object> score = requestAttractivenessScore(user.getId(), frontImageBase64, segmentKey);
                if (score == null) {
                    failed++;
                    continue;
                }

                double visualScore = clamp01(readDouble(score, "score", 0.5));
                double confidence = clamp01(readDouble(score, "confidence", 0.0));
                String provider = readString(score, "provider", DEFAULT_PROVIDER);
                String modelVersion = readString(score, "model_version", DEFAULT_MODEL_VERSION);

                if (qualityAssessment.isPresent()) {
                    FaceQualityScoringService.FaceQualityAssessment assessment = qualityAssessment.get();
                    double blendWeight = clamp01(qualityBlendWeight) * clamp01(assessment.confidence());
                    visualScore = clamp01(((1.0 - blendWeight) * visualScore) + (blendWeight * clamp01(assessment.qualityScore())));
                    confidence = clamp01(((1.0 - blendWeight) * confidence) + (blendWeight * clamp01(assessment.confidence())));
                    provider = provider + "+quality";
                }

                if (qdrantAttractivenessHintEnabled && qdrantIntegrationService.isEnabled()) {
                    Optional<QdrantAttractivenessHint> hint = qdrantIntegrationService.getAttractivenessHint(
                            user.getId(),
                            segmentKey,
                            qdrantAttractivenessCollection,
                            qdrantAttractivenessHintMaxDelta
                    );
                    if (hint.isPresent()) {
                        QdrantAttractivenessHint h = hint.get();
                        visualScore = clamp01(visualScore + h.boost());
                        provider = provider + "+" + h.source();
                    }
                }

                UserVisualAttractiveness row = current == null ? new UserVisualAttractiveness() : current;
                row.setUserId(user.getId());
                row.setVisualScore(visualScore);
                row.setConfidence(confidence);
                row.setSourceProvider(truncate(provider, 80));
                row.setModelVersion(truncate(modelVersion, 80));
                visualRepo.save(row);
                processed++;
            } catch (Exception e) {
                failed++;
                LOGGER.warn("Failed to sync visual attractiveness for user {}", user.getId(), e);
            }
        }

        LOGGER.info("Visual attractiveness sync finished: processed={}, skippedFresh={}, skippedNoMedia={}, skippedPolicy={}, skippedQuality={}, failed={}",
                processed, skippedFresh, skippedNoMedia, skippedPolicy, skippedQuality, failed);
    }

    private boolean isFresh(UserVisualAttractiveness row, Instant refreshCutoff) {
        return row != null
                && row.getUpdatedAt() != null
                && row.getUpdatedAt().toInstant().isAfter(refreshCutoff);
    }

    private Optional<String> loadProfilePictureAsBase64(User user) {
        try {
            UserProfilePicture picture = user.getProfilePicture();
            if (picture == null || picture.getS3Key() == null) {
                return Optional.empty();
            }
            byte[] bytes = s3StorageService.downloadMedia(picture.getS3Key());
            if (bytes == null || bytes.length == 0) {
                return Optional.empty();
            }
            String mime = picture.getBinMime();
            if (mime == null || mime.isBlank()) {
                mime = "image/jpeg";
            }
            return Optional.of("data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes));
        } catch (Exception e) {
            LOGGER.debug("Could not load profile picture for user {}", user.getId(), e);
            return Optional.empty();
        }
    }

    private Map<String, Object> requestAttractivenessScore(Long userId, String frontImageBase64, String segmentKey) {
        if (javaMediaBackendService.isEnabled()) {
            byte[] frontBytes = decodeBase64Image(frontImageBase64);
            return javaMediaBackendService.scoreAttractiveness(frontBytes, null);
        }

        String url = mediaServiceUrl + "/attractiveness/score";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", userId);
        payload.put("front_image_base64", frontImageBase64);
        if (segmentKey != null && !segmentKey.isBlank()) {
            payload.put("segment_key", segmentKey);
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        return response.getBody();
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

    private boolean isAttractivenessEnabledByUnleash(User user, String segmentKey) {
        Map<String, String> context = new HashMap<>();
        context.put("userId", String.valueOf(user.getId()));
        context.put("segmentKey", segmentKey == null ? "unknown" : segmentKey);
        return unleashIntegrationService.isFeatureEnabled(unleashFlagName, context);
    }

    private double readDouble(Map<String, Object> source, String key, double fallback) {
        Object raw = source.get(key);
        if (raw instanceof Number n) {
            return n.doubleValue();
        }
        return fallback;
    }

    private String readString(Map<String, Object> source, String key, String fallback) {
        Object raw = source.get(key);
        if (raw instanceof String s && !s.isBlank()) {
            return s;
        }
        return fallback;
    }

    private double clamp01(double value) {
        if (!Double.isFinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    private String truncate(String value, int maxLen) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String out = value.trim();
        if (out.length() <= maxLen) {
            return out;
        }
        return out.substring(0, maxLen);
    }
}
