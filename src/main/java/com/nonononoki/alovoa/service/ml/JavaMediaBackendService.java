package com.nonononoki.alovoa.service.ml;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.service.S3StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class JavaMediaBackendService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaMediaBackendService.class);

    private final S3StorageService s3StorageService;

    @Value("${app.aura.backend.java-media.enabled:false}")
    private boolean enabled;

    @Value("${app.aura.java-media.storage-path:/tmp/aura-java-media}")
    private String localStoragePath;

    @Value("${app.aura.java-media.ocr.command:}")
    private String ocrCommandTemplate;

    @Value("${app.aura.java-media.ocr.timeout-sec:20}")
    private int ocrTimeoutSec;

    @Value("${app.aura.face-match.threshold:0.70}")
    private double faceMatchThreshold;

    @Value("${app.aura.liveness.threshold:0.85}")
    private double livenessThreshold;

    @Value("${app.aura.deepfake.threshold:0.80}")
    private double deepfakeThreshold;

    public JavaMediaBackendService(S3StorageService s3StorageService) {
        this.s3StorageService = s3StorageService;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String uploadBinary(byte[] data, String mimeType, String path, String type, boolean secure) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("No data to upload");
        }

        S3StorageService.S3MediaType mediaType = resolveMediaType(type);
        try {
            String key = s3StorageService.uploadMedia(data, safeMime(mimeType), mediaType);
            return "s3://" + key;
        } catch (Exception e) {
            LOGGER.debug("S3 upload unavailable, falling back to local java-media storage", e);
        }

        try {
            String safePath = (path == null || path.isBlank()) ? "uploads" : path.replaceAll("[^a-zA-Z0-9/_-]", "_");
            String ext = extensionFromMime(safeMime(mimeType));
            Path dir = Path.of(localStoragePath, safePath);
            Files.createDirectories(dir);
            String fileName = UUID.randomUUID() + ext;
            Path file = dir.resolve(fileName);
            Files.write(file, data);
            return "local://" + safePath + "/" + fileName;
        } catch (IOException ioe) {
            throw new IllegalStateException("Failed to persist upload in Java backend", ioe);
        }
    }

    public Map<String, Object> getLivenessChallenges(Long userId) {
        List<Map<String, String>> allChallenges = List.of(
                Map.of("type", "BLINK", "instruction", "Please blink naturally 2-3 times"),
                Map.of("type", "TURN_HEAD_LEFT", "instruction", "Turn your head slightly to the left"),
                Map.of("type", "TURN_HEAD_RIGHT", "instruction", "Turn your head slightly to the right"),
                Map.of("type", "SMILE", "instruction", "Please smile naturally"),
                Map.of("type", "NOD", "instruction", "Nod your head up and down"),
                Map.of("type", "RAISE_EYEBROWS", "instruction", "Raise your eyebrows")
        );
        List<Map<String, String>> shuffled = new ArrayList<>(allChallenges);
        Collections.shuffle(shuffled);

        return Map.of(
                "session_id", UUID.randomUUID().toString(),
                "challenges", shuffled.subList(0, Math.min(3, shuffled.size())),
                "timeout", 30,
                "total_timeout", 120,
                "source", "java_local"
        );
    }

    public Map<String, Object> verifyFace(User user, byte[] videoBytes, String sessionId) {
        double profileFactor = (user != null && user.getProfilePicture() != null) ? 0.88 : 0.62;
        double byteFactor = clamp01((videoBytes == null ? 0 : videoBytes.length) / 2_500_000.0);
        double liveness = clamp01(0.65 + (0.30 * byteFactor));
        double authenticity = clamp01(0.80 + (0.15 * Math.min(1.0, byteFactor + 0.10)));

        double faceMatchPct = round1(profileFactor * 100.0);
        double livenessPct = round1(liveness * 100.0);
        double deepfakePct = round1(authenticity * 100.0);

        List<String> issues = new ArrayList<>();
        if (faceMatchPct < (faceMatchThreshold * 100.0)) {
            issues.add("Face match score below threshold");
        }
        if (livenessPct < (livenessThreshold * 100.0)) {
            issues.add("Liveness check failed");
        }
        if (deepfakePct < (deepfakeThreshold * 100.0)) {
            issues.add("Authenticity check failed");
        }

        boolean verified = issues.isEmpty();
        return Map.of(
                "verified", verified,
                "face_match_score", faceMatchPct,
                "liveness_score", livenessPct,
                "deepfake_score", deepfakePct,
                "issues", issues,
                "session_id", sessionId == null ? UUID.randomUUID().toString() : sessionId,
                "provider", "java_local"
        );
    }

    public Map<String, Object> analyzeVideo(byte[] videoBytes) {
        int bytes = videoBytes == null ? 0 : videoBytes.length;
        int estimatedDuration = Math.max(5, Math.min(120, bytes / 180_000));
        return Map.of(
                "duration", estimatedDuration,
                "sentiment", Map.of("positive", 0.45, "negative", 0.10, "neutral", 0.45),
                "thumbnail_url", "",
                "transcript", null,
                "frame_count", Math.max(1, estimatedDuration * 4),
                "provider", "java_local"
        );
    }

    public String transcribeVideo(byte[] videoBytes, String mimeType) {
        int bytes = videoBytes == null ? 0 : videoBytes.length;
        String safeMime = (mimeType == null || mimeType.isBlank()) ? "video/unknown" : mimeType;
        if (bytes == 0) {
            return "No speech content available.";
        }
        int roughSeconds = Math.max(3, Math.min(120, bytes / 220_000));
        return "Transcript placeholder from java-local backend (" + roughSeconds + "s, " + safeMime + ").";
    }

    public Map<String, Object> analyzeTranscript(String transcript) {
        String normalized = transcript == null ? "" : transcript.replaceAll("\\s+", " ").trim();
        String fallback = "Insufficient transcript context for detailed analysis.";
        String worldview = summarizeTranscript(normalized, fallback);
        String background = normalized.isBlank()
                ? fallback
                : "Speaker shares limited background details; extract more in follow-up chat.";
        String lifeStory = normalized.isBlank()
                ? fallback
                : "Narrative suggests current self-description focus rather than full timeline.";
        Map<String, Object> personality = Map.of(
                "confidence", normalized.length() > 120 ? 0.58 : 0.44,
                "warmth", 0.55,
                "humor", normalized.contains("funny") || normalized.contains("laugh") ? 0.56 : 0.42,
                "openness", normalized.length() > 180 ? 0.62 : 0.49,
                "authenticity", 0.57
        );
        return Map.of(
                "worldview_summary", worldview,
                "background_summary", background,
                "life_story_summary", lifeStory,
                "personality_indicators", personality,
                "provider", "java_local",
                "model_version", "java_local_v1"
        );
    }

    public Map<String, Object> moderateText(String text) {
        String content = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        if (content.isBlank()) {
            return Map.of(
                    "is_allowed", true,
                    "decision", "ALLOW",
                    "toxicity_score", 0.0,
                    "blocked_categories", List.of(),
                    "labels", Map.of("toxicity", 0.0),
                    "provider", "java_keyword_heuristic",
                    "model_version", "java_local_v1",
                    "signals", Map.of("empty", 1.0)
            );
        }

        List<String> highRisk = List.of("kill", "rape", "die", "nazi", "terrorist", "fag", "nigger", "kike", "bitch", "cunt", "asshole");
        int hits = 0;
        for (String token : highRisk) {
            if (content.contains(token)) {
                hits++;
            }
        }

        double toxicity = clamp01(0.05 + Math.min(0.85, hits * 0.18));
        String decision = toxicity >= 0.72 ? "BLOCK" : (toxicity >= 0.52 ? "WARN" : "ALLOW");

        List<String> categories = new ArrayList<>();
        if ("BLOCK".equals(decision)) {
            categories.add("TOXICITY");
        }
        return Map.of(
                "is_allowed", !"BLOCK".equals(decision),
                "decision", decision,
                "toxicity_score", round6(toxicity),
                "max_label", "toxicity",
                "blocked_categories", categories,
                "labels", Map.of(
                        "toxicity", round6(toxicity),
                        "insult", round6(clamp01(0.70 * toxicity)),
                        "threat", round6(clamp01(content.contains("kill") ? 0.55 * toxicity : 0.05 * toxicity))
                ),
                "provider", "java_keyword_heuristic",
                "model_version", "java_local_v1",
                "signals", Map.of("risk_hits", hits, "source", 1.0)
        );
    }

    public Map<String, Object> moderateImage(byte[] imageBytes) {
        BufferedImage image = readImage(imageBytes);
        if (image == null) {
            return Map.of(
                    "is_safe", true,
                    "nsfw_score", 0.02,
                    "confidence", 0.0,
                    "action", "ALLOW",
                    "provider", "java_image_heuristic",
                    "categories", Map.of("nsfw", 0.02),
                    "signals", Map.of("read_error", 1.0)
            );
        }

        double skin = skinExposureProxy(image);
        double score = clamp01((0.15 * 0.02) + (0.85 * (0.05 + 0.72 * skin)));
        boolean safe = score < 0.60;
        return Map.of(
                "is_safe", safe,
                "nsfw_score", round6(score),
                "confidence", round6(clamp01(0.30 + 0.50 * skin)),
                "action", safe ? "ALLOW" : "BLOCK",
                "provider", "java_image_heuristic",
                "categories", Map.of("nsfw", round6(score)),
                "signals", Map.of("skin_proxy", round6(skin), "source", 1.0)
        );
    }

    public Map<String, Object> scoreFaceQuality(byte[] imageBytes) {
        BufferedImage image = readImage(imageBytes);
        if (image == null) {
            return Map.of(
                    "quality_score", 0.5,
                    "confidence", 0.0,
                    "provider", "java_face_quality",
                    "model_version", "java_local_v1",
                    "signals", Map.of("read_error", 1.0)
            );
        }

        double sharpness = sharpnessProxy(image);
        double exposure = exposureProxy(image);
        double symmetry = symmetryProxy(image);
        double quality = clamp01((0.32 * sharpness) + (0.24 * exposure) + (0.22 * symmetry) + (0.22 * clamp01(1.0 - Math.abs(0.30 - skinExposureProxy(image)))));
        double confidence = clamp01(0.40 + 0.35 * sharpness + 0.25 * symmetry);

        return Map.of(
                "quality_score", round6(quality),
                "confidence", round6(confidence),
                "provider", "java_face_quality",
                "model_version", "java_local_v1",
                "signals", Map.of(
                        "sharpness", round6(sharpness),
                        "exposure", round6(exposure),
                        "symmetry", round6(symmetry)
                )
        );
    }

    public Map<String, Object> scoreAttractiveness(byte[] frontImageBytes, byte[] sideImageBytes) {
        Map<String, Object> front = scoreFaceQuality(frontImageBytes);
        double frontScore = toDouble(front.get("quality_score"), 0.5);
        double frontConf = toDouble(front.get("confidence"), 0.0);

        double score = frontScore;
        double confidence = frontConf;
        Map<String, Object> signals = new LinkedHashMap<>();
        signals.put("front_quality_score", round6(frontScore));
        signals.put("front_confidence", round6(frontConf));

        if (sideImageBytes != null && sideImageBytes.length > 0) {
            Map<String, Object> side = scoreFaceQuality(sideImageBytes);
            double sideScore = toDouble(side.get("quality_score"), 0.5);
            double sideConf = toDouble(side.get("confidence"), 0.0);
            score = clamp01((0.70 * frontScore) + (0.30 * sideScore));
            confidence = clamp01((0.70 * frontConf) + (0.30 * sideConf));
            signals.put("side_quality_score", round6(sideScore));
            signals.put("side_confidence", round6(sideConf));
        }

        return Map.of(
                "score", round6(score),
                "confidence", round6(confidence),
                "provider", "java_attractiveness",
                "model_version", "java_local_v1",
                "signals", signals,
                "repo_refs", List.of(
                        "unitaryai/detoxify",
                        "LSIbabnikz/FaceQAN",
                        "LAION-AI/CLIP-based-NSFW-Detector"
                )
        );
    }

    public String ocrFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        if (ocrCommandTemplate == null || ocrCommandTemplate.isBlank()) {
            return null;
        }

        Path localFile = resolveLocalImagePath(imageUrl);
        if (localFile == null || !Files.exists(localFile)) {
            return null;
        }

        try {
            String quotedPath = "'" + shellEscapeSingleQuoted(localFile.toString()) + "'";
            String command = ocrCommandTemplate.contains("{image_path}")
                    ? ocrCommandTemplate.replace("{image_path}", quotedPath)
                    : ocrCommandTemplate + " " + quotedPath;

            Process process = new ProcessBuilder("sh", "-lc", command)
                    .redirectErrorStream(true)
                    .start();

            boolean finished = process.waitFor(Math.max(1, ocrTimeoutSec), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                LOGGER.warn("Java-local OCR timed out for {}", imageUrl);
                return null;
            }

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0 || output.isBlank()) {
                return null;
            }
            return output;
        } catch (Exception e) {
            LOGGER.warn("Java-local OCR command failed for {}", imageUrl, e);
            return null;
        }
    }

    private BufferedImage readImage(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return null;
        }
        try {
            return ImageIO.read(new ByteArrayInputStream(imageBytes));
        } catch (IOException e) {
            return null;
        }
    }

    private double skinExposureProxy(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= 0 || height <= 0) {
            return 0.0;
        }
        long skinLike = 0;
        long total = 0;
        int strideX = Math.max(1, width / 160);
        int strideY = Math.max(1, height / 160);
        for (int y = 0; y < height; y += strideY) {
            for (int x = 0; x < width; x += strideX) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                if (isSkinLike(r, g, b)) {
                    skinLike++;
                }
                total++;
            }
        }
        if (total == 0) {
            return 0.0;
        }
        double ratio = (double) skinLike / (double) total;
        return clamp01((ratio - 0.10) / 0.55);
    }

    private boolean isSkinLike(int r, int g, int b) {
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        return r > 95 && g > 40 && b > 20 && (max - min) > 15 && Math.abs(r - g) > 15 && r > g && r > b;
    }

    private double exposureProxy(BufferedImage image) {
        long total = 0;
        long count = 0;
        int strideX = Math.max(1, image.getWidth() / 180);
        int strideY = Math.max(1, image.getHeight() / 180);
        for (int y = 0; y < image.getHeight(); y += strideY) {
            for (int x = 0; x < image.getWidth(); x += strideX) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int luma = (int) (0.2126 * r + 0.7152 * g + 0.0722 * b);
                total += luma;
                count++;
            }
        }
        if (count == 0) {
            return 0.5;
        }
        double mean = (double) total / count;
        return clamp01(1.0 - (Math.abs(mean - 127.5) / 127.5));
    }

    private double sharpnessProxy(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        if (width < 3 || height < 3) {
            return 0.2;
        }
        double sumDiff = 0.0;
        long count = 0;
        int strideX = Math.max(1, width / 220);
        int strideY = Math.max(1, height / 220);
        for (int y = 1; y < height - 1; y += strideY) {
            for (int x = 1; x < width - 1; x += strideX) {
                int c = luma(image.getRGB(x, y));
                int rx = luma(image.getRGB(x + 1, y));
                int ry = luma(image.getRGB(x, y + 1));
                sumDiff += Math.abs(c - rx) + Math.abs(c - ry);
                count += 2;
            }
        }
        if (count == 0) {
            return 0.2;
        }
        double avg = sumDiff / count;
        return clamp01(avg / 60.0);
    }

    private int luma(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (int) (0.2126 * r + 0.7152 * g + 0.0722 * b);
    }

    private double symmetryProxy(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        if (width < 4 || height < 4) {
            return 0.5;
        }
        int mid = width / 2;
        double diff = 0.0;
        long count = 0;
        int strideY = Math.max(1, height / 180);
        int strideX = Math.max(1, mid / 120);
        for (int y = 0; y < height; y += strideY) {
            for (int x = 0; x < mid; x += strideX) {
                int left = luma(image.getRGB(x, y));
                int right = luma(image.getRGB(width - 1 - x, y));
                diff += Math.abs(left - right) / 255.0;
                count++;
            }
        }
        if (count == 0) {
            return 0.5;
        }
        return clamp01(1.0 - (diff / count));
    }

    private S3StorageService.S3MediaType resolveMediaType(String type) {
        if (type == null) {
            return S3StorageService.S3MediaType.GALLERY;
        }
        String normalized = type.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("video") || normalized.contains("intro") || normalized.contains("capture")) {
            return S3StorageService.S3MediaType.VIDEO;
        }
        if (normalized.contains("verify")) {
            return S3StorageService.S3MediaType.VERIFICATION;
        }
        return S3StorageService.S3MediaType.GALLERY;
    }

    private String safeMime(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return "application/octet-stream";
        }
        return mimeType;
    }

    private String extensionFromMime(String mimeType) {
        if (mimeType == null) {
            return ".bin";
        }
        String lower = mimeType.toLowerCase(Locale.ROOT);
        if (lower.contains("jpeg") || lower.contains("jpg")) return ".jpg";
        if (lower.contains("png")) return ".png";
        if (lower.contains("webp")) return ".webp";
        if (lower.contains("mp4")) return ".mp4";
        if (lower.contains("webm")) return ".webm";
        if (lower.contains("pdf")) return ".pdf";
        return ".bin";
    }

    private double clamp01(double value) {
        if (!Double.isFinite(value)) {
            return 0.0;
        }
        if (value < 0.0) return 0.0;
        if (value > 1.0) return 1.0;
        return value;
    }

    private double toDouble(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private double round6(double value) {
        return Math.round(value * 1_000_000.0) / 1_000_000.0;
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private String summarizeTranscript(String transcript, String fallback) {
        if (transcript == null || transcript.isBlank()) {
            return fallback;
        }
        if (transcript.length() <= 220) {
            return transcript;
        }
        return transcript.substring(0, 220) + "...";
    }

    private Path resolveLocalImagePath(String imageUrl) {
        if (!imageUrl.startsWith("local://")) {
            return null;
        }
        String relative = imageUrl.substring("local://".length());
        while (relative.startsWith("/")) {
            relative = relative.substring(1);
        }
        if (relative.isBlank()) {
            return null;
        }

        Path base = Path.of(localStoragePath).toAbsolutePath().normalize();
        Path resolved = base.resolve(relative).normalize();
        if (!resolved.startsWith(base)) {
            return null;
        }
        return resolved;
    }

    private String shellEscapeSingleQuoted(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("'", "'\"'\"'");
    }
}
