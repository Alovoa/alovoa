package com.nonononoki.alovoa.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.model.VideoAnalysisResult;
import com.nonononoki.alovoa.service.ml.JavaMediaBackendService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Anthropic Claude-based AI provider for transcript analysis.
 * Note: Claude doesn't have native speech-to-text, so transcription
 * falls back to the local media service.
 */
@Service
@ConditionalOnProperty(name = "app.aura.ai.provider", havingValue = "claude")
public class ClaudeProvider implements AiAnalysisProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClaudeProvider.class);
    private static final String PROVIDER_NAME = "claude";

    private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    @Value("${app.aura.ai.claude.api-key:}")
    private String apiKey;

    @Value("${app.aura.ai.claude.model:claude-3-opus-20240229}")
    private String model;

    @Value("${app.aura.media-service.url:http://localhost:8001}")
    private String mediaServiceUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final JavaMediaBackendService javaMediaBackendService;

    public ClaudeProvider(RestTemplate restTemplate,
                          ObjectMapper objectMapper,
                          JavaMediaBackendService javaMediaBackendService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.javaMediaBackendService = javaMediaBackendService;
    }

    @Override
    public String transcribeVideo(byte[] videoData, String mimeType) throws AiProviderException {
        if (javaMediaBackendService.isEnabled()) {
            return javaMediaBackendService.transcribeVideo(videoData, mimeType);
        }

        // Claude doesn't have native transcription - use legacy media service
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Convert video to base64 for transport
            String base64Video = java.util.Base64.getEncoder().encodeToString(videoData);

            Map<String, Object> request = Map.of(
                    "video_data", base64Video,
                    "mime_type", mimeType
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    mediaServiceUrl + "/video/transcribe",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().get("transcript");
            } else {
                throw new AiProviderException(PROVIDER_NAME, "Transcription failed via media service");
            }

        } catch (Exception e) {
            LOGGER.error("Transcription failed", e);
            throw new AiProviderException(PROVIDER_NAME, "Transcription failed: " + e.getMessage(), e);
        }
    }

    @Override
    public VideoAnalysisResult analyzeTranscript(String transcript) throws AiProviderException {
        if (!isAvailable()) {
            throw new AiProviderException(PROVIDER_NAME, "Claude API key not configured");
        }

        try {
            String systemPrompt = buildSystemPrompt();
            String userPrompt = "Analyze the following video introduction transcript:\n\n" + transcript;

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("max_tokens", 2048);  // Increased for inference data
            requestBody.put("system", systemPrompt);
            requestBody.put("messages", List.of(
                    Map.of("role", "user", "content", userPrompt)
            ));

            HttpHeaders headers = new HttpHeaders();
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", ANTHROPIC_VERSION);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    CLAUDE_API_URL,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseAnalysisResponse(response.getBody());
            } else {
                throw new AiProviderException(PROVIDER_NAME, "Analysis failed: " + response.getStatusCode());
            }

        } catch (Exception e) {
            LOGGER.error("Claude analysis failed", e);
            throw new AiProviderException(PROVIDER_NAME, "Analysis failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean isAvailable() {
        return StringUtils.hasText(apiKey);
    }

    private String buildSystemPrompt() {
        return """
            You are an AI assistant that analyzes dating app video introductions.
            Extract the following information from the transcript and respond ONLY with valid JSON:

            {
              "worldview_summary": "A 2-3 sentence summary of their values, beliefs, and outlook on life",
              "background_summary": "A 2-3 sentence summary of their background (education, career, where from)",
              "life_story_summary": "A 2-3 sentence narrative of their life journey and key experiences",
              "personality_indicators": {
                "confidence": 0.0-1.0,
                "warmth": 0.0-1.0,
                "humor": 0.0-1.0,
                "openness": 0.0-1.0,
                "authenticity": 0.0-1.0
              },
              "inferred_big_five": {
                "openness": 0-100,
                "conscientiousness": 0-100,
                "extraversion": 0-100,
                "agreeableness": 0-100,
                "neuroticism": 0-100
              },
              "inferred_values": {
                "progressive": 0-100,
                "egalitarian": 0-100
              },
              "inferred_lifestyle": {
                "social": 0-100,
                "health": 0-100,
                "workLife": 0-100,
                "finance": 0-100
              },
              "inferred_attachment": {
                "anxiety": 0-100,
                "avoidance": 0-100
              },
              "inferred_attachment_style": "SECURE|ANXIOUS|AVOIDANT|FEARFUL",
              "suggested_dealbreakers": ["dealbreaker_key1", "dealbreaker_key2"],
              "confidence_scores": {
                "bigFive": 0.0-1.0,
                "values": 0.0-1.0,
                "lifestyle": 0.0-1.0,
                "attachment": 0.0-1.0,
                "dealbreakers": 0.0-1.0
              },
              "low_confidence_areas": ["area1", "area2"],
              "overall_confidence": 0.0-1.0
            }

            SCORING GUIDELINES:
            - Big Five: Analyze speech patterns, vocabulary, topics, emotional expression
              - Openness: creativity, curiosity, new experiences
              - Conscientiousness: organization, responsibility, goal-orientation
              - Extraversion: energy from social interaction, talkativeness
              - Agreeableness: cooperation, empathy, conflict avoidance
              - Neuroticism: emotional volatility, anxiety, mood swings
            - Values: Analyze expressed beliefs, priorities, social attitudes
              - Progressive: openness to social change, modern views
              - Egalitarian: equality beliefs, fairness orientation
            - Lifestyle: Analyze mentioned activities, routines, priorities
              - Social: frequency of social activities
              - Health: fitness, diet, wellness focus
              - WorkLife: career vs personal life balance
              - Finance: financial responsibility, spending habits
            - Attachment: Analyze relationship descriptions, intimacy comfort
              - Anxiety: fear of abandonment, need for reassurance
              - Avoidance: discomfort with closeness, independence
              - Style: SECURE (low both), ANXIOUS (high anxiety, low avoidance),
                       AVOIDANT (low anxiety, high avoidance), FEARFUL (high both)
            - Dealbreakers: Common keys include smoking, drugs, kids_must_have,
              kids_must_not_have, religious_match, political_match, monogamy_required

            Be objective and extract only what is stated or clearly implied.
            If information is not available, use 50 as neutral score.
            Respond with ONLY the JSON object, no other text.
            """;
    }

    @SuppressWarnings("unchecked")
    private VideoAnalysisResult parseAnalysisResponse(Map<String, Object> response) throws AiProviderException {
        try {
            List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
            if (content == null || content.isEmpty()) {
                throw new AiProviderException(PROVIDER_NAME, "No content in response");
            }

            String text = (String) content.get(0).get("text");

            // Extract JSON from response (Claude might include extra text)
            int jsonStart = text.indexOf('{');
            int jsonEnd = text.lastIndexOf('}') + 1;
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                text = text.substring(jsonStart, jsonEnd);
            }

            Map<String, Object> parsed = objectMapper.readValue(text, Map.class);

            // Build base result
            VideoAnalysisResult.VideoAnalysisResultBuilder builder = VideoAnalysisResult.builder()
                    .worldviewSummary((String) parsed.get("worldview_summary"))
                    .backgroundSummary((String) parsed.get("background_summary"))
                    .lifeStorySummary((String) parsed.get("life_story_summary"))
                    .personalityIndicators((Map<String, Object>) parsed.get("personality_indicators"))
                    .providerName(PROVIDER_NAME);

            // Add inference data if present
            if (parsed.containsKey("inferred_big_five")) {
                builder.inferredBigFive(convertToDoubleMap(parsed.get("inferred_big_five")));
            }
            if (parsed.containsKey("inferred_values")) {
                builder.inferredValues(convertToDoubleMap(parsed.get("inferred_values")));
            }
            if (parsed.containsKey("inferred_lifestyle")) {
                builder.inferredLifestyle(convertToDoubleMap(parsed.get("inferred_lifestyle")));
            }
            if (parsed.containsKey("inferred_attachment")) {
                builder.inferredAttachment(convertToDoubleMap(parsed.get("inferred_attachment")));
            }
            if (parsed.containsKey("inferred_attachment_style")) {
                builder.inferredAttachmentStyle((String) parsed.get("inferred_attachment_style"));
            }
            if (parsed.containsKey("suggested_dealbreakers")) {
                builder.suggestedDealbreakers((List<String>) parsed.get("suggested_dealbreakers"));
            }
            if (parsed.containsKey("confidence_scores")) {
                builder.confidenceScores(convertToDoubleMap(parsed.get("confidence_scores")));
            }
            if (parsed.containsKey("low_confidence_areas")) {
                builder.lowConfidenceAreas((List<String>) parsed.get("low_confidence_areas"));
            }
            if (parsed.containsKey("overall_confidence")) {
                Object confidence = parsed.get("overall_confidence");
                if (confidence instanceof Number) {
                    builder.overallConfidence(((Number) confidence).doubleValue());
                }
            }

            return builder.build();

        } catch (Exception e) {
            throw new AiProviderException(PROVIDER_NAME, "Failed to parse response: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> convertToDoubleMap(Object obj) {
        if (obj == null) return null;
        Map<String, Object> input = (Map<String, Object>) obj;
        Map<String, Double> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            if (entry.getValue() instanceof Number) {
                result.put(entry.getKey(), ((Number) entry.getValue()).doubleValue());
            }
        }
        return result;
    }
}
