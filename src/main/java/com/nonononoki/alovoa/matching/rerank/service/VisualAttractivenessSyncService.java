package com.nonononoki.alovoa.matching.rerank.service;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.matching.UserVisualAttractiveness;
import com.nonononoki.alovoa.entity.user.UserProfilePicture;
import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.repo.matching.UserVisualAttractivenessRepository;
import com.nonononoki.alovoa.service.S3StorageService;
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

    @Value("${app.aura.media-service.url:http://localhost:8001}")
    private String mediaServiceUrl;

    @Value("${app.aura.attractiveness.max-users-per-run:500}")
    private int maxUsersPerRun;

    @Value("${app.aura.attractiveness.rescore-days:14}")
    private int rescoreDays;

    public VisualAttractivenessSyncService(UserRepository userRepo,
                                           UserVisualAttractivenessRepository visualRepo,
                                           S3StorageService s3StorageService,
                                           RestTemplate restTemplate) {
        this.userRepo = userRepo;
        this.visualRepo = visualRepo;
        this.s3StorageService = s3StorageService;
        this.restTemplate = restTemplate;
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
        int failed = 0;

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
                Map<String, Object> score = requestAttractivenessScore(user.getId(), frontImageBase64);
                if (score == null) {
                    failed++;
                    continue;
                }

                UserVisualAttractiveness row = current == null ? new UserVisualAttractiveness() : current;
                row.setUserId(user.getId());
                row.setVisualScore(clamp01(readDouble(score, "score", 0.5)));
                row.setConfidence(clamp01(readDouble(score, "confidence", 0.0)));
                row.setSourceProvider(readString(score, "provider", DEFAULT_PROVIDER));
                row.setModelVersion(readString(score, "model_version", DEFAULT_MODEL_VERSION));
                visualRepo.save(row);
                processed++;
            } catch (Exception e) {
                failed++;
                LOGGER.warn("Failed to sync visual attractiveness for user {}", user.getId(), e);
            }
        }

        LOGGER.info("Visual attractiveness sync finished: processed={}, skippedFresh={}, skippedNoMedia={}, failed={}",
                processed, skippedFresh, skippedNoMedia, failed);
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

    private Map<String, Object> requestAttractivenessScore(Long userId, String frontImageBase64) {
        String url = mediaServiceUrl + "/attractiveness/score";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", userId);
        payload.put("front_image_base64", frontImageBase64);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        return response.getBody();
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
        return Math.max(0.0, Math.min(1.0, value));
    }
}
