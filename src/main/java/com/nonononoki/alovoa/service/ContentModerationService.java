package com.nonononoki.alovoa.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.ContentModerationEvent;
import com.nonononoki.alovoa.model.ModerationResult;
import com.nonononoki.alovoa.repo.ContentModerationEventRepository;
import com.nonononoki.alovoa.service.ml.JavaMediaBackendService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Base64;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class ContentModerationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentModerationService.class);

    @Value("${app.moderation.enabled:true}")
    private boolean moderationEnabled;

    @Value("${app.moderation.api-key:}")
    private String perspectiveApiKey;

    @Value("${app.moderation.toxicity-threshold:0.7}")
    private double toxicityThreshold;

    @Value("${app.moderation.insult-threshold:0.8}")
    private double insultThreshold;

    @Value("${app.moderation.profanity-threshold:0.9}")
    private double profanityThreshold;

    @Value("${app.moderation.image.enabled:false}")
    private boolean imageModerationEnabled;

    @Value("${app.moderation.image-threshold:0.6}")
    private double imageModerationThreshold;

    @Value("${app.moderation.text-service.enabled:false}")
    private boolean textModerationServiceEnabled;

    @Value("${app.moderation.text-service.url:${app.aura.media-service.url:http://localhost:8001}}")
    private String textModerationServiceUrl;

    @Value("${app.aura.media-service.url:http://localhost:8001}")
    private String mediaServiceUrl;

    @Autowired
    private ContentModerationEventRepository moderationEventRepo;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private JavaMediaBackendService javaMediaBackendService;

    private Set<String> blockedWords;
    private Set<String> warningWords;
    private List<Pattern> blockedPatterns;

    @PostConstruct
    public void init() {
        loadKeywords();
    }

    private void loadKeywords() {
        try {
            ClassPathResource resource = new ClassPathResource("data/moderation-keywords.json");
            JsonNode root = objectMapper.readTree(resource.getInputStream());

            blockedWords = new HashSet<>();
            warningWords = new HashSet<>();
            blockedPatterns = new ArrayList<>();

            if (root.has("blockedWords")) {
                root.get("blockedWords").forEach(node -> blockedWords.add(node.asText().toLowerCase()));
            }

            if (root.has("warningWords")) {
                root.get("warningWords").forEach(node -> warningWords.add(node.asText().toLowerCase()));
            }

            if (root.has("blockedPatterns")) {
                root.get("blockedPatterns").forEach(node ->
                    blockedPatterns.add(Pattern.compile(node.asText(), Pattern.CASE_INSENSITIVE))
                );
            }

            LOGGER.info("Loaded moderation keywords: {} blocked words, {} warning words, {} patterns",
                    blockedWords.size(), warningWords.size(), blockedPatterns.size());
        } catch (IOException e) {
            LOGGER.warn("Failed to load moderation keywords, using empty lists", e);
            blockedWords = new HashSet<>();
            warningWords = new HashSet<>();
            blockedPatterns = new ArrayList<>();
        }
    }

    /**
     * Moderate content using Perspective API or fallback to keyword filter
     */
    public ModerationResult moderateContent(String content) {
        return moderateContent(content, null, null);
    }

    /**
     * Moderate content and log the event
     */
    public ModerationResult moderateContent(String content, User user, String contentType) {
        if (!moderationEnabled) {
            return ModerationResult.allowed();
        }

        if (content == null || content.trim().isEmpty()) {
            return ModerationResult.allowed();
        }

        ModerationResult result = null;
        String provider = "keyword_filter";
        String modelVersion = null;
        String sourceMode = "local";
        String signalJson = null;

        if (textModerationServiceEnabled) {
            try {
                TextModerationServiceDecision remote = moderateWithTextService(content, contentType);
                if (remote != null) {
                    result = remote.result;
                    provider = remote.provider;
                    modelVersion = remote.modelVersion;
                    sourceMode = "remote_text_service";
                    signalJson = remote.signalJson;
                }
            } catch (Exception e) {
                LOGGER.warn("Text moderation service failed, falling back to existing moderation chain", e);
            }
        }

        // Existing moderation chain fallback
        if (result == null) {
            if (perspectiveApiKey != null && !perspectiveApiKey.isBlank()) {
                try {
                    result = moderateWithPerspectiveAPI(content);
                    provider = "perspective_api";
                    sourceMode = "api";
                } catch (Exception e) {
                    LOGGER.warn("Perspective API moderation failed, falling back to keyword filter", e);
                    result = keywordFilter(content);
                    provider = "keyword_filter";
                    sourceMode = "fallback";
                }
            } else {
                result = keywordFilter(content);
                provider = "keyword_filter";
                sourceMode = "fallback";
            }
        }

        // Log moderation event if user and content type provided
        if (user != null && contentType != null) {
            logModerationEvent(user, contentType, result, provider, modelVersion, sourceMode, signalJson);
        }

        return result;
    }

    /**
     * Moderate an image by calling local media-service moderation endpoint.
     * Safe fallback: allow content if endpoint is disabled/unavailable.
     */
    public ModerationResult moderateImage(byte[] imageBytes, String mimeType, User user, String contentType) {
        if (!moderationEnabled || !imageModerationEnabled) {
            return ModerationResult.allowed();
        }
        if (imageBytes == null || imageBytes.length == 0) {
            return ModerationResult.allowed();
        }

        if (javaMediaBackendService.isEnabled()) {
            try {
                Map<String, Object> payload = javaMediaBackendService.moderateImage(imageBytes);
                JsonNode root = objectMapper.valueToTree(payload);
                ModerationResult result = parseImageModerationResult(root);
                if (user != null && contentType != null) {
                    String provider = root.path("provider").asText("java_image_heuristic");
                    String modelVersion = root.path("model_version").isTextual() ? root.path("model_version").asText() : "java_local_v1";
                    String signalJson = null;
                    try {
                        JsonNode signalsNode = root.path("signals");
                        if (signalsNode != null && !signalsNode.isMissingNode()) {
                            signalJson = objectMapper.writeValueAsString(signalsNode);
                        }
                    } catch (Exception ignored) {
                        signalJson = null;
                    }
                    logModerationEvent(user, contentType, result, provider, modelVersion, "java_local_image", signalJson);
                }
                return result;
            } catch (Exception e) {
                LOGGER.warn("Java-local image moderation failed; allowing image as safe fallback", e);
                return ModerationResult.allowed();
            }
        }

        String safeMime = (mimeType == null || mimeType.isBlank()) ? "image/jpeg" : mimeType;
        String imageBase64 = "data:" + safeMime + ";base64," + Base64.getEncoder().encodeToString(imageBytes);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("image_base64", imageBase64);
        requestBody.put("image_type", contentType == null ? "PROFILE" : contentType);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    mediaServiceUrl + "/moderation/image",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return ModerationResult.allowed();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            String provider = root.path("provider").asText("image_moderation");
            String modelVersion = root.path("model_version").isTextual() ? root.path("model_version").asText() : null;
            String signalJson = null;
            try {
                JsonNode signalsNode = root.path("signals");
                if (signalsNode != null && !signalsNode.isMissingNode()) {
                    signalJson = objectMapper.writeValueAsString(signalsNode);
                }
            } catch (Exception ignored) {
                signalJson = null;
            }

            ModerationResult result = parseImageModerationResult(root);

            if (user != null && contentType != null) {
                logModerationEvent(user, contentType, result, provider, modelVersion, "remote_image_service", signalJson);
            }
            return result;
        } catch (Exception e) {
            LOGGER.warn("Image moderation endpoint failed; allowing image as safe fallback", e);
            return ModerationResult.allowed();
        }
    }

    /**
     * Use Perspective API for moderation
     */
    private ModerationResult moderateWithPerspectiveAPI(String content) {
        String url = "https://commentanalyzer.googleapis.com/v1alpha1/comments:analyze?key=" + perspectiveApiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Build request body
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> comment = new HashMap<>();
        comment.put("text", content);
        requestBody.put("comment", comment);

        Map<String, Object> requestedAttributes = new HashMap<>();
        requestedAttributes.put("TOXICITY", new HashMap<>());
        requestedAttributes.put("INSULT", new HashMap<>());
        requestedAttributes.put("PROFANITY", new HashMap<>());
        requestedAttributes.put("THREAT", new HashMap<>());
        requestedAttributes.put("IDENTITY_ATTACK", new HashMap<>());
        requestBody.put("requestedAttributes", requestedAttributes);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode attributeScores = root.path("attributeScores");

                double toxicityScore = attributeScores.path("TOXICITY").path("summaryScore").path("value").asDouble(0.0);
                double insultScore = attributeScores.path("INSULT").path("summaryScore").path("value").asDouble(0.0);
                double profanityScore = attributeScores.path("PROFANITY").path("summaryScore").path("value").asDouble(0.0);
                double threatScore = attributeScores.path("THREAT").path("summaryScore").path("value").asDouble(0.0);
                double identityAttackScore = attributeScores.path("IDENTITY_ATTACK").path("summaryScore").path("value").asDouble(0.0);

                List<String> flaggedCategories = new ArrayList<>();
                boolean blocked = false;

                if (toxicityScore >= toxicityThreshold) {
                    flaggedCategories.add("TOXICITY");
                    blocked = true;
                }
                if (insultScore >= insultThreshold) {
                    flaggedCategories.add("INSULT");
                    blocked = true;
                }
                if (profanityScore >= profanityThreshold) {
                    flaggedCategories.add("PROFANITY");
                    blocked = true;
                }
                if (threatScore >= 0.7) {
                    flaggedCategories.add("THREAT");
                    blocked = true;
                }
                if (identityAttackScore >= 0.7) {
                    flaggedCategories.add("IDENTITY_ATTACK");
                    blocked = true;
                }

                if (blocked) {
                    return ModerationResult.blocked(
                            toxicityScore,
                            flaggedCategories,
                            "Content flagged by automated moderation: " + String.join(", ", flaggedCategories)
                    );
                } else {
                    return ModerationResult.allowed();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error calling Perspective API", e);
            throw new RuntimeException("Perspective API error", e);
        }

        return ModerationResult.allowed();
    }

    /**
     * Keyword-based fallback filter
     */
    private ModerationResult keywordFilter(String content) {
        if (content == null || content.trim().isEmpty()) {
            return ModerationResult.allowed();
        }

        String lowerContent = content.toLowerCase();
        List<String> flaggedCategories = new ArrayList<>();

        // Check blocked words
        for (String word : blockedWords) {
            if (lowerContent.contains(word)) {
                flaggedCategories.add("BLOCKED_WORD");
                return ModerationResult.blocked(
                        1.0,
                        flaggedCategories,
                        "Content contains prohibited language"
                );
            }
        }

        // Check blocked patterns
        for (Pattern pattern : blockedPatterns) {
            if (pattern.matcher(content).find()) {
                flaggedCategories.add("BLOCKED_PATTERN");
                return ModerationResult.blocked(
                        1.0,
                        flaggedCategories,
                        "Content matches prohibited pattern"
                );
            }
        }

        // Check warning words (log but don't block)
        for (String word : warningWords) {
            if (lowerContent.contains(word)) {
                flaggedCategories.add("WARNING_WORD");
                LOGGER.info("Content contains warning word: {}", word);
                // Don't block, just log
            }
        }

        return ModerationResult.allowed();
    }

    /**
     * Log moderation event to database
     */
    private void logModerationEvent(
            User user,
            String contentType,
            ModerationResult result,
            String provider,
            String modelVersion,
            String sourceMode,
            String signalJson
    ) {
        try {
            ContentModerationEvent event = new ContentModerationEvent();
            event.setUser(user);
            event.setContentType(contentType);
            event.setToxicityScore(result.getToxicityScore());
            event.setFlaggedCategories(String.join(",", result.getFlaggedCategories()));
            event.setProvider(provider);
            event.setModelVersion(modelVersion);
            event.setSourceMode(sourceMode);
            event.setSignalJson(signalJson);
            event.setBlocked(!result.isAllowed());
            event.setCreatedAt(new Date());

            moderationEventRepo.save(event);
        } catch (Exception e) {
            LOGGER.error("Failed to log moderation event", e);
            // Don't fail the moderation check if logging fails
        }
    }

    /**
     * Get moderation statistics for a user
     */
    public long getBlockedContentCount(User user) {
        return moderationEventRepo.countByUserAndBlocked(user, true);
    }

    private TextModerationServiceDecision moderateWithTextService(String content, String contentType) {
        if (javaMediaBackendService.isEnabled()) {
            try {
                Map<String, Object> payload = javaMediaBackendService.moderateText(content);
                JsonNode root = objectMapper.valueToTree(payload);
                return parseTextModerationDecision(root);
            } catch (Exception e) {
                LOGGER.warn("Java-local text moderation failed", e);
                return null;
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("text", content);
        if (contentType != null && !contentType.isBlank()) {
            requestBody.put("content_type", contentType);
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                textModerationServiceUrl + "/moderation/text",
                HttpMethod.POST,
                entity,
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return null;
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            LOGGER.warn("Failed parsing text moderation service response", e);
            return null;
        }
        return parseTextModerationDecision(root);
    }

    private ModerationResult parseImageModerationResult(JsonNode root) {
        boolean isSafe = root.path("is_safe").asBoolean(true);
        double nsfwScore = root.path("nsfw_score").asDouble(0.0);

        List<String> flaggedCategories = new ArrayList<>();
        JsonNode categories = root.path("categories");
        if (categories != null && categories.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = categories.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                if (entry.getValue().isNumber()) {
                    double value = entry.getValue().asDouble(0.0);
                    if (value >= imageModerationThreshold) {
                        flaggedCategories.add(entry.getKey().toUpperCase(Locale.ROOT));
                    }
                }
            }
        }

        if (!isSafe || nsfwScore >= imageModerationThreshold) {
            if (flaggedCategories.isEmpty()) {
                flaggedCategories.add("NSFW");
            }
            return ModerationResult.blocked(
                    nsfwScore,
                    flaggedCategories,
                    "Image flagged by visual moderation"
            );
        }
        return ModerationResult.allowed();
    }

    private TextModerationServiceDecision parseTextModerationDecision(JsonNode root) {
        boolean isAllowed = root.path("is_allowed").asBoolean(true);
        double toxicityScore = root.path("toxicity_score").asDouble(0.0);
        List<String> categories = new ArrayList<>();
        JsonNode blockedCategories = root.path("blocked_categories");
        if (blockedCategories != null && blockedCategories.isArray()) {
            blockedCategories.forEach(node -> {
                if (node.isTextual()) {
                    categories.add(node.asText().toUpperCase(Locale.ROOT));
                }
            });
        }

        ModerationResult result;
        if (!isAllowed) {
            if (categories.isEmpty()) {
                categories.add("TOXICITY");
            }
            String reason = root.path("reason").isTextual()
                    ? root.path("reason").asText()
                    : "Content flagged by text moderation service";
            result = ModerationResult.blocked(toxicityScore, categories, reason);
        } else {
            result = ModerationResult.allowed();
        }

        String signalJson = null;
        try {
            JsonNode signalsNode = root.path("signals");
            if (signalsNode != null && !signalsNode.isMissingNode()) {
                signalJson = objectMapper.writeValueAsString(signalsNode);
            }
        } catch (Exception ignored) {
            signalJson = null;
        }

        TextModerationServiceDecision decision = new TextModerationServiceDecision();
        decision.result = result;
        decision.provider = root.path("provider").asText("text_moderation_service");
        decision.modelVersion = root.path("model_version").isTextual() ? root.path("model_version").asText() : null;
        decision.signalJson = signalJson;
        return decision;
    }

    private static class TextModerationServiceDecision {
        private ModerationResult result;
        private String provider;
        private String modelVersion;
        private String signalJson;
    }
}
