package com.nonononoki.alovoa.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.model.VideoAnalysisResult;
import com.nonononoki.alovoa.service.ml.JavaMediaBackendService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Google Gemini AI provider for video analysis.
 * Uses Gemini Pro for text analysis and Google Cloud Speech-to-Text for transcription.
 *
 * Gemini can also process video directly with multimodal input.
 */
@Service
@ConditionalOnProperty(name = "app.aura.ai.provider", havingValue = "gemini")
public class GeminiProvider implements AiAnalysisProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeminiProvider.class);
    private static final String PROVIDER_NAME = "gemini";

    // Gemini API endpoint (generative language API)
    private static final String GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta";

    @Value("${app.aura.ai.gemini.api-key:}")
    private String apiKey;

    @Value("${app.aura.ai.gemini.model:gemini-1.5-pro}")
    private String model;

    @Value("${app.aura.media-service.url:http://localhost:8001}")
    private String mediaServiceUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final JavaMediaBackendService javaMediaBackendService;

    public GeminiProvider(RestTemplate restTemplate,
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

        // Option 1: Use Gemini's multimodal capabilities to transcribe
        // Option 2: Fall back to media service for transcription

        // For now, use media service as it's more reliable for pure transcription
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String base64Video = Base64.getEncoder().encodeToString(videoData);

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
            LOGGER.warn("Media service transcription failed, trying Gemini multimodal: {}", e.getMessage());
            return transcribeWithGeminiMultimodal(videoData, mimeType);
        }
    }

    /**
     * Use Gemini's multimodal capabilities to transcribe video directly.
     */
    private String transcribeWithGeminiMultimodal(byte[] videoData, String mimeType) throws AiProviderException {
        if (!isAvailable()) {
            throw new AiProviderException(PROVIDER_NAME, "Gemini API key not configured");
        }

        try {
            String base64Video = Base64.getEncoder().encodeToString(videoData);

            Map<String, Object> requestBody = buildMultimodalRequest(
                    base64Video,
                    mimeType,
                    "Please transcribe all spoken words in this video accurately. " +
                    "Output only the transcription, nothing else."
            );

            String apiUrl = String.format("%s/models/%s:generateContent?key=%s",
                    GEMINI_API_BASE, model, apiKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return extractTextFromGeminiResponse(response.getBody());
            } else {
                throw new AiProviderException(PROVIDER_NAME, "Transcription failed: " + response.getStatusCode());
            }

        } catch (Exception e) {
            LOGGER.error("Gemini multimodal transcription failed", e);
            throw new AiProviderException(PROVIDER_NAME, "Transcription failed: " + e.getMessage(), e);
        }
    }

    @Override
    public VideoAnalysisResult analyzeTranscript(String transcript) throws AiProviderException {
        if (!isAvailable()) {
            throw new AiProviderException(PROVIDER_NAME, "Gemini API key not configured");
        }

        try {
            String systemPrompt = buildSystemPrompt();
            String userPrompt = "Analyze the following video introduction transcript:\n\n" + transcript;

            Map<String, Object> requestBody = buildTextRequest(systemPrompt + "\n\n" + userPrompt);

            String apiUrl = String.format("%s/models/%s:generateContent?key=%s",
                    GEMINI_API_BASE, model, apiKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl,
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
            LOGGER.error("Gemini analysis failed", e);
            throw new AiProviderException(PROVIDER_NAME, "Analysis failed: " + e.getMessage(), e);
        }
    }

    /**
     * Analyze video directly using Gemini's multimodal capabilities.
     * This can extract both transcript and analysis in one call.
     */
    @Override
    public VideoAnalysisResult analyzeVideo(byte[] videoData, String mimeType) throws AiProviderException {
        if (!isAvailable()) {
            throw new AiProviderException(PROVIDER_NAME, "Gemini API key not configured");
        }

        try {
            String base64Video = Base64.getEncoder().encodeToString(videoData);

            String prompt = """
                Analyze this video introduction for a dating app. First transcribe what is said,
                then analyze the content. Respond in this exact JSON format:

                {
                  "transcript": "The full transcription of what was said",
                  "worldview_summary": "A 2-3 sentence summary of their values, beliefs, and outlook on life",
                  "background_summary": "A 2-3 sentence summary of their background (education, career, where from)",
                  "life_story_summary": "A 2-3 sentence narrative of their life journey and key experiences",
                  "personality_indicators": {
                    "confidence": 0.0-1.0,
                    "warmth": 0.0-1.0,
                    "humor": 0.0-1.0,
                    "openness": 0.0-1.0,
                    "authenticity": 0.0-1.0
                  }
                }

                Be objective and extract only what is stated or clearly implied.
                If information is not available, indicate "Not mentioned in video".
                Respond with ONLY the JSON object.
                """;

            Map<String, Object> requestBody = buildMultimodalRequest(base64Video, mimeType, prompt);

            String apiUrl = String.format("%s/models/%s:generateContent?key=%s",
                    GEMINI_API_BASE, model, apiKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseMultimodalAnalysisResponse(response.getBody());
            } else {
                throw new AiProviderException(PROVIDER_NAME, "Analysis failed: " + response.getStatusCode());
            }

        } catch (Exception e) {
            LOGGER.error("Gemini multimodal analysis failed, falling back to standard flow", e);
            // Fall back to standard two-step process
            return AiAnalysisProvider.super.analyzeVideo(videoData, mimeType);
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

    private Map<String, Object> buildTextRequest(String prompt) {
        return Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.7,
                        "maxOutputTokens", 2048
                )
        );
    }

    private Map<String, Object> buildMultimodalRequest(String base64Video, String mimeType, String prompt) {
        return Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of(
                                        "inline_data", Map.of(
                                                "mime_type", mimeType,
                                                "data", base64Video
                                        )
                                ),
                                Map.of("text", prompt)
                        ))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.7,
                        "maxOutputTokens", 2048
                )
        );
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
              }
            }

            Be objective and extract only what is stated or clearly implied.
            If information is not available, indicate "Not mentioned in video".
            """;
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromGeminiResponse(Map<String, Object> response) throws AiProviderException {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                throw new AiProviderException(PROVIDER_NAME, "No candidates in response");
            }

            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");

            return (String) parts.get(0).get("text");

        } catch (Exception e) {
            throw new AiProviderException(PROVIDER_NAME, "Failed to extract text: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private VideoAnalysisResult parseAnalysisResponse(Map<String, Object> response) throws AiProviderException {
        try {
            String text = extractTextFromGeminiResponse(response);

            // Extract JSON from response
            int jsonStart = text.indexOf('{');
            int jsonEnd = text.lastIndexOf('}') + 1;
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                text = text.substring(jsonStart, jsonEnd);
            }

            Map<String, Object> parsed = objectMapper.readValue(text, Map.class);

            return VideoAnalysisResult.builder()
                    .worldviewSummary((String) parsed.get("worldview_summary"))
                    .backgroundSummary((String) parsed.get("background_summary"))
                    .lifeStorySummary((String) parsed.get("life_story_summary"))
                    .personalityIndicators((Map<String, Object>) parsed.get("personality_indicators"))
                    .providerName(PROVIDER_NAME)
                    .build();

        } catch (Exception e) {
            throw new AiProviderException(PROVIDER_NAME, "Failed to parse response: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private VideoAnalysisResult parseMultimodalAnalysisResponse(Map<String, Object> response) throws AiProviderException {
        try {
            String text = extractTextFromGeminiResponse(response);

            // Extract JSON from response
            int jsonStart = text.indexOf('{');
            int jsonEnd = text.lastIndexOf('}') + 1;
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                text = text.substring(jsonStart, jsonEnd);
            }

            Map<String, Object> parsed = objectMapper.readValue(text, Map.class);

            return VideoAnalysisResult.builder()
                    .transcript((String) parsed.get("transcript"))
                    .worldviewSummary((String) parsed.get("worldview_summary"))
                    .backgroundSummary((String) parsed.get("background_summary"))
                    .lifeStorySummary((String) parsed.get("life_story_summary"))
                    .personalityIndicators((Map<String, Object>) parsed.get("personality_indicators"))
                    .providerName(PROVIDER_NAME)
                    .build();

        } catch (Exception e) {
            throw new AiProviderException(PROVIDER_NAME, "Failed to parse response: " + e.getMessage(), e);
        }
    }
}
