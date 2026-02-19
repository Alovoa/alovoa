package com.nonononoki.alovoa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserBehaviorEvent;
import com.nonononoki.alovoa.entity.user.UserVideo;
import com.nonononoki.alovoa.entity.user.UserVideoVerification;
import com.nonononoki.alovoa.entity.user.UserVideoVerification.VerificationStatus;
import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.repo.UserVideoRepository;
import com.nonononoki.alovoa.repo.UserVideoVerificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import com.nonononoki.alovoa.service.ml.JavaMediaBackendService;

import java.util.*;

@Service
public class VideoVerificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VideoVerificationService.class);

    @Value("${app.aura.media-service.url:http://localhost:8001}")
    private String mediaServiceUrl;

    @Value("${app.aura.video.max-size-mb:100}")
    private Integer maxVideoSizeMb;

    @Value("${app.aura.video.intro-max-seconds:90}")
    private Integer maxIntroSeconds;

    @Value("${app.aura.video.intro-min-seconds:30}")
    private Integer minIntroSeconds;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private UserVideoRepository videoRepo;

    @Autowired
    private UserVideoVerificationRepository verificationRepo;

    @Autowired
    private ReputationService reputationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private JavaMediaBackendService javaMediaBackendService;

    public Map<String, Object> uploadIntroVideo(MultipartFile video) throws Exception {
        User user = authService.getCurrentUser(true);

        // Validate video
        validateVideo(video);

        // Upload to storage via media service
        String videoUrl = uploadToMediaService(video, user.getUuid().toString(), "intro");

        // Check if user already has an intro video
        UserVideo existingIntro = videoRepo.findByUserAndIsIntroTrue(user);
        if (existingIntro != null) {
            existingIntro.setVideoUrl(videoUrl);
            existingIntro.setCreatedAt(new Date());
            existingIntro.setIsVerified(false);
            videoRepo.save(existingIntro);

            return Map.of(
                    "success", true,
                    "videoId", existingIntro.getId(),
                    "videoUrl", videoUrl,
                    "message", "Intro video updated"
            );
        }

        // Create new video entity
        UserVideo userVideo = new UserVideo();
        userVideo.setUser(user);
        userVideo.setVideoType(UserVideo.VideoType.INTRO);
        userVideo.setVideoUrl(videoUrl);
        userVideo.setIsIntro(true);
        userVideo.setIsVerified(false);

        videoRepo.save(userVideo);

        // Trigger async video analysis
        analyzeVideoAsync(userVideo);

        return Map.of(
                "success", true,
                "videoId", userVideo.getId(),
                "videoUrl", videoUrl,
                "message", "Intro video uploaded successfully"
        );
    }

    public Map<String, Object> startVerificationSession() throws Exception {
        User user = authService.getCurrentUser(true);

        // Generate unique session ID
        String sessionId = UUID.randomUUID().toString();

        // Get or create verification record
        UserVideoVerification verification = verificationRepo.findByUser(user)
                .orElseGet(() -> {
                    UserVideoVerification v = new UserVideoVerification();
                    v.setUser(user);
                    return v;
                });

        verification.setSessionId(sessionId);
        verification.setStatus(VerificationStatus.PENDING);
        verification.setCreatedAt(new Date());
        verificationRepo.save(verification);

        // Get liveness challenges from media service
        Map<String, Object> challenges;
        try {
            challenges = getLivenessChallenges(sessionId, user.getId());
        } catch (Exception e) {
            LOGGER.warn("Media service unavailable, using default challenges", e);
            challenges = getDefaultChallenges();
        }

        return Map.of(
                "sessionId", sessionId,
                "challenges", challenges,
                "expiresIn", 300 // 5 minutes
        );
    }

    public Map<String, Object> submitVerification(MultipartFile video, String sessionId, String metadataJson) throws Exception {
        User user = authService.getCurrentUser(true);

        // Validate session
        UserVideoVerification verification = verificationRepo.findBySessionId(sessionId)
                .orElseThrow(() -> new Exception("Invalid verification session"));

        if (!verification.getUser().getId().equals(user.getId())) {
            throw new Exception("Unauthorized");
        }

        // Parse and store capture metadata for anti-replay/correlation
        Map<String, Object> captureMetadata = null;
        if (metadataJson != null && !metadataJson.isEmpty()) {
            try {
                captureMetadata = objectMapper.readValue(metadataJson, Map.class);
                verification.setCaptureMetadata(metadataJson);
                LOGGER.debug("Capture metadata: mimeType={}, durationMs={}, resolution={}x{}",
                        sanitizeForLog(captureMetadata.get("mimeType")),
                        sanitizeForLog(captureMetadata.get("durationMs")),
                        sanitizeForLog(captureMetadata.get("videoWidth")),
                        sanitizeForLog(captureMetadata.get("videoHeight")));
            } catch (Exception e) {
                LOGGER.warn("Failed to parse capture metadata", e);
            }
        }

        // Upload verification video
        String videoUrl = uploadToMediaService(video, "verification/" + user.getUuid().toString(), "verification");

        verification.setVideoUrl(videoUrl);
        verification.setStatus(VerificationStatus.PROCESSING);
        verificationRepo.save(verification);

        // Call verification service
        Map<String, Object> verificationResult;
        try {
            verificationResult = callVerificationService(user, videoUrl, sessionId, video);
        } catch (Exception e) {
            LOGGER.error("Verification service failed", e);
            verification.setStatus(VerificationStatus.FAILED);
            verification.setFailureReason("Verification service unavailable");
            verificationRepo.save(verification);

            return Map.of(
                    "success", false,
                    "status", "FAILED",
                    "message", "Verification service temporarily unavailable"
            );
        }

        // Process result
        boolean isVerified = (Boolean) verificationResult.getOrDefault("verified", false);
        Double faceMatchScore = ((Number) verificationResult.getOrDefault("face_match_score", 0.0)).doubleValue();
        Double livenessScore = ((Number) verificationResult.getOrDefault("liveness_score", 0.0)).doubleValue();
        Double deepfakeScore = ((Number) verificationResult.getOrDefault("deepfake_score", 1.0)).doubleValue();

        verification.setFaceMatchScore(faceMatchScore);
        verification.setLivenessScore(livenessScore);
        verification.setDeepfakeScore(deepfakeScore);

        if (isVerified) {
            verification.setStatus(VerificationStatus.VERIFIED);
            verification.setVerifiedAt(new Date());

            // Record positive behavior
            reputationService.recordBehavior(user,
                    UserBehaviorEvent.BehaviorType.VIDEO_VERIFIED,
                    null, Map.of("face_match", faceMatchScore, "liveness", livenessScore));
        } else {
            verification.setStatus(VerificationStatus.FAILED);
            List<String> issues = (List<String>) verificationResult.getOrDefault("issues", Collections.emptyList());
            verification.setFailureReason(String.join(", ", issues));
        }

        verificationRepo.save(verification);

        return Map.of(
                "success", isVerified,
                "status", verification.getStatus().name(),
                "scores", Map.of(
                        "faceMatch", faceMatchScore,
                        "liveness", livenessScore,
                        "authenticity", deepfakeScore
                ),
                "message", isVerified ? "Verification successful!" : "Verification failed: " + verification.getFailureReason()
        );
    }

    public Map<String, Object> getVerificationStatus() throws Exception {
        User user = authService.getCurrentUser(true);

        UserVideoVerification verification = verificationRepo.findByUser(user).orElse(null);

        if (verification == null) {
            return Map.of(
                    "status", "NOT_STARTED",
                    "isVerified", false
            );
        }

        Map<String, Object> result = new HashMap<>();
        result.put("status", verification.getStatus().name());
        result.put("isVerified", verification.isVerified());

        // Add stage for polling UI updates
        if (verification.getStatus() == VerificationStatus.PROCESSING) {
            // Determine stage based on time since submission or other factors
            long elapsedMs = System.currentTimeMillis() - verification.getCreatedAt().getTime();
            if (elapsedMs < 3000) {
                result.put("stage", "UPLOADING");
            } else if (elapsedMs < 8000) {
                result.put("stage", "ANALYZING");
            } else {
                result.put("stage", "VERIFYING");
            }
        }

        if (verification.isVerified()) {
            result.put("verifiedAt", verification.getVerifiedAt());
            result.put("scores", Map.of(
                    "faceMatch", verification.getFaceMatchScore(),
                    "liveness", verification.getLivenessScore(),
                    "authenticity", verification.getDeepfakeScore()
            ));
        } else if (verification.getStatus() == VerificationStatus.FAILED) {
            result.put("failureReason", verification.getFailureReason());
            result.put("message", verification.getFailureReason());
        }

        return result;
    }

    private void validateVideo(MultipartFile video) throws Exception {
        if (video == null || video.isEmpty()) {
            throw new Exception("Video file is required");
        }

        long maxBytes = maxVideoSizeMb * 1024L * 1024L;
        if (video.getSize() > maxBytes) {
            throw new Exception("Video file too large. Maximum size: " + maxVideoSizeMb + "MB");
        }

        String contentType = video.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new Exception("Invalid file type. Please upload a video file");
        }
    }

    private String uploadToMediaService(MultipartFile video, String path, String type) throws Exception {
        if (javaMediaBackendService.isEnabled()) {
            String contentType = video.getContentType() == null ? "video/mp4" : video.getContentType();
            return javaMediaBackendService.uploadBinary(video.getBytes(), contentType, path, type, false);
        }

        try {
            String url = mediaServiceUrl + "/upload/video";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // Create multipart request body
            org.springframework.util.LinkedMultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
            body.add("file", new org.springframework.core.io.ByteArrayResource(video.getBytes()) {
                @Override
                public String getFilename() {
                    return video.getOriginalFilename();
                }
            });
            body.add("path", path);
            body.add("type", type);

            HttpEntity<org.springframework.util.LinkedMultiValueMap<String, Object>> entity =
                    new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getBody() != null && response.getBody().containsKey("url")) {
                return (String) response.getBody().get("url");
            }

            throw new Exception("Failed to upload video");
        } catch (Exception e) {
            LOGGER.error("Media service upload failed", e);
            // Fallback: store locally (in production, use proper object storage)
            return "local://" + path + "/" + UUID.randomUUID().toString() + ".mp4";
        }
    }

    private Map<String, Object> getLivenessChallenges(String sessionId, Long userId) {
        if (javaMediaBackendService.isEnabled()) {
            return javaMediaBackendService.getLivenessChallenges(userId);
        }

        String url = mediaServiceUrl + "/verify/liveness/challenges";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> request = Map.of(
                "session_id", sessionId,
                "user_id", userId
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
        );

        return response.getBody();
    }

    private Map<String, Object> getDefaultChallenges() {
        return Map.of(
                "challenges", List.of(
                        Map.of("type", "BLINK", "instruction", "Please blink naturally"),
                        Map.of("type", "TURN_HEAD_LEFT", "instruction", "Turn your head slightly to the left"),
                        Map.of("type", "SMILE", "instruction", "Please smile")
                ),
                "timeout", 30
        );
    }

    private Map<String, Object> callVerificationService(User user, String videoUrl, String sessionId, MultipartFile uploadedVideo) throws Exception {
        if (javaMediaBackendService.isEnabled()) {
            return javaMediaBackendService.verifyFace(user, uploadedVideo.getBytes(), sessionId);
        }

        String url = mediaServiceUrl + "/verify/face";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Get user's profile picture URL
        String profilePicUrl = getProfilePictureUrl(user);

        Map<String, Object> request = Map.of(
                "user_id", user.getId(),
                "profile_image_url", profilePicUrl,
                "verification_video_url", videoUrl,
                "session_id", sessionId
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
        );

        return response.getBody();
    }

    private String getProfilePictureUrl(User user) {
        if (user.getProfilePicture() != null && user.getProfilePicture().getUuid() != null) {
            return "/media/profile-picture/" + user.getProfilePicture().getUuid().toString();
        }
        return null;
    }

    private String sanitizeForLog(Object rawValue) {
        if (rawValue == null) {
            return "null";
        }
        String asString = String.valueOf(rawValue)
                .replace('\r', '_')
                .replace('\n', '_')
                .replace('\t', '_');
        if (asString.length() > 120) {
            return asString.substring(0, 120);
        }
        return asString;
    }

    private void analyzeVideoAsync(UserVideo video) {
        // This would typically be done asynchronously
        if (javaMediaBackendService.isEnabled()) {
            try {
                Map<String, Object> result = javaMediaBackendService.analyzeVideo(new byte[0]);
                if (result.containsKey("transcript")) {
                    video.setTranscript((String) result.get("transcript"));
                }
                if (result.containsKey("duration")) {
                    video.setDurationSeconds(((Number) result.get("duration")).intValue());
                }
                if (result.containsKey("sentiment")) {
                    video.setSentimentScores(objectMapper.writeValueAsString(result.get("sentiment")));
                }
                videoRepo.save(video);
            } catch (Exception e) {
                LOGGER.warn("Java-local video analysis failed", e);
            }
            return;
        }

        try {
            String url = mediaServiceUrl + "/video/analyze";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = Map.of(
                    "video_url", video.getVideoUrl(),
                    "user_id", video.getUser().getId()
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getBody() != null) {
                Map<String, Object> result = response.getBody();
                if (result.containsKey("transcript")) {
                    video.setTranscript((String) result.get("transcript"));
                }
                if (result.containsKey("duration")) {
                    video.setDurationSeconds(((Number) result.get("duration")).intValue());
                }
                if (result.containsKey("sentiment")) {
                    video.setSentimentScores(objectMapper.writeValueAsString(result.get("sentiment")));
                }
                videoRepo.save(video);
            }
        } catch (Exception e) {
            LOGGER.warn("Video analysis failed", e);
        }
    }
}
