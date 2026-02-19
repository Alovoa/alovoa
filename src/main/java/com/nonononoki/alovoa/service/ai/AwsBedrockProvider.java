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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * AWS Bedrock AI provider supporting multiple foundation models:
 * - Amazon Titan
 * - Anthropic Claude (on Bedrock)
 * - AI21 Labs Jurassic
 * - Cohere Command
 * - Meta Llama 2
 * - Mistral
 *
 * Uses AWS Signature Version 4 for authentication.
 */
@Service
@ConditionalOnProperty(name = "app.aura.ai.provider", havingValue = "bedrock")
public class AwsBedrockProvider implements AiAnalysisProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsBedrockProvider.class);
    private static final String PROVIDER_NAME = "bedrock";

    private static final String SERVICE = "bedrock-runtime";
    private static final String ALGORITHM = "AWS4-HMAC-SHA256";

    @Value("${app.aura.ai.bedrock.access-key:}")
    private String accessKey;

    @Value("${app.aura.ai.bedrock.secret-key:}")
    private String secretKey;

    @Value("${app.aura.ai.bedrock.region:us-east-1}")
    private String region;

    @Value("${app.aura.ai.bedrock.model:anthropic.claude-3-sonnet-20240229-v1:0}")
    private String modelId;

    @Value("${app.aura.media-service.url:http://localhost:8001}")
    private String mediaServiceUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final JavaMediaBackendService javaMediaBackendService;

    public AwsBedrockProvider(RestTemplate restTemplate,
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

        // AWS Transcribe could be used here, but for simplicity use legacy media service
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
            LOGGER.error("Transcription failed", e);
            throw new AiProviderException(PROVIDER_NAME, "Transcription failed: " + e.getMessage(), e);
        }
    }

    @Override
    public VideoAnalysisResult analyzeTranscript(String transcript) throws AiProviderException {
        if (!isAvailable()) {
            throw new AiProviderException(PROVIDER_NAME, "AWS Bedrock credentials not configured");
        }

        try {
            String requestBody = buildRequestBody(transcript);
            String endpoint = String.format("https://bedrock-runtime.%s.amazonaws.com/model/%s/invoke",
                    region, modelId);

            HttpHeaders headers = createSignedHeaders(endpoint, requestBody);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    endpoint,
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
            LOGGER.error("Bedrock analysis failed", e);
            throw new AiProviderException(PROVIDER_NAME, "Analysis failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME + ":" + getModelShortName();
    }

    @Override
    public boolean isAvailable() {
        return StringUtils.hasText(accessKey) && StringUtils.hasText(secretKey);
    }

    private String getModelShortName() {
        if (modelId.contains("claude")) return "claude";
        if (modelId.contains("titan")) return "titan";
        if (modelId.contains("llama")) return "llama";
        if (modelId.contains("mistral")) return "mistral";
        if (modelId.contains("cohere")) return "cohere";
        if (modelId.contains("jurassic")) return "jurassic";
        return "unknown";
    }

    private String buildRequestBody(String transcript) throws Exception {
        String systemPrompt = buildSystemPrompt();
        String userPrompt = "Analyze the following video introduction transcript:\n\n" + transcript;

        // Different models have different request formats
        if (modelId.contains("anthropic.claude")) {
            return buildClaudeRequest(systemPrompt, userPrompt);
        } else if (modelId.contains("amazon.titan")) {
            return buildTitanRequest(systemPrompt, userPrompt);
        } else if (modelId.contains("meta.llama")) {
            return buildLlamaRequest(systemPrompt, userPrompt);
        } else if (modelId.contains("mistral")) {
            return buildMistralRequest(systemPrompt, userPrompt);
        } else if (modelId.contains("cohere")) {
            return buildCohereRequest(systemPrompt, userPrompt);
        } else {
            // Default to Claude format
            return buildClaudeRequest(systemPrompt, userPrompt);
        }
    }

    private String buildClaudeRequest(String systemPrompt, String userPrompt) throws Exception {
        Map<String, Object> request = Map.of(
                "anthropic_version", "bedrock-2023-05-31",
                "max_tokens", 2048,
                "system", systemPrompt,
                "messages", List.of(
                        Map.of("role", "user", "content", userPrompt)
                )
        );
        return objectMapper.writeValueAsString(request);
    }

    private String buildTitanRequest(String systemPrompt, String userPrompt) throws Exception {
        Map<String, Object> request = Map.of(
                "inputText", systemPrompt + "\n\nUser: " + userPrompt + "\n\nAssistant:",
                "textGenerationConfig", Map.of(
                        "maxTokenCount", 2048,
                        "temperature", 0.7,
                        "topP", 0.9
                )
        );
        return objectMapper.writeValueAsString(request);
    }

    private String buildLlamaRequest(String systemPrompt, String userPrompt) throws Exception {
        String prompt = String.format("""
            <s>[INST] <<SYS>>
            %s
            <</SYS>>

            %s [/INST]
            """, systemPrompt, userPrompt);

        Map<String, Object> request = Map.of(
                "prompt", prompt,
                "max_gen_len", 2048,
                "temperature", 0.7,
                "top_p", 0.9
        );
        return objectMapper.writeValueAsString(request);
    }

    private String buildMistralRequest(String systemPrompt, String userPrompt) throws Exception {
        Map<String, Object> request = Map.of(
                "prompt", "<s>[INST] " + systemPrompt + "\n\n" + userPrompt + " [/INST]",
                "max_tokens", 2048,
                "temperature", 0.7,
                "top_p", 0.9
        );
        return objectMapper.writeValueAsString(request);
    }

    private String buildCohereRequest(String systemPrompt, String userPrompt) throws Exception {
        Map<String, Object> request = Map.of(
                "prompt", systemPrompt + "\n\n" + userPrompt,
                "max_tokens", 2048,
                "temperature", 0.7,
                "p", 0.9
        );
        return objectMapper.writeValueAsString(request);
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
            Respond with ONLY the JSON object, no other text.
            """;
    }

    @SuppressWarnings("unchecked")
    private VideoAnalysisResult parseAnalysisResponse(Map<String, Object> response) throws AiProviderException {
        try {
            String text;

            // Different models have different response formats
            if (modelId.contains("anthropic.claude")) {
                List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
                text = (String) content.get(0).get("text");
            } else if (modelId.contains("amazon.titan")) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
                text = (String) results.get(0).get("outputText");
            } else if (modelId.contains("meta.llama")) {
                text = (String) response.get("generation");
            } else if (modelId.contains("mistral")) {
                List<Map<String, Object>> outputs = (List<Map<String, Object>>) response.get("outputs");
                text = (String) outputs.get(0).get("text");
            } else if (modelId.contains("cohere")) {
                List<Map<String, Object>> generations = (List<Map<String, Object>>) response.get("generations");
                text = (String) generations.get(0).get("text");
            } else {
                // Try Claude format as default
                List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
                text = (String) content.get(0).get("text");
            }

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
                    .providerName(getProviderName())
                    .build();

        } catch (Exception e) {
            throw new AiProviderException(PROVIDER_NAME, "Failed to parse response: " + e.getMessage(), e);
        }
    }

    /**
     * Create AWS Signature Version 4 signed headers.
     */
    private HttpHeaders createSignedHeaders(String endpoint, String requestBody) throws Exception {
        URI uri = new URI(endpoint);
        String host = uri.getHost();
        String path = uri.getPath();

        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String amzDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
        String dateStamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // Create canonical request
        String payloadHash = sha256Hex(requestBody);
        String canonicalHeaders = "content-type:application/json\n" +
                "host:" + host + "\n" +
                "x-amz-date:" + amzDate + "\n";
        String signedHeaders = "content-type;host;x-amz-date";

        String canonicalRequest = "POST\n" +
                path + "\n" +
                "\n" +
                canonicalHeaders + "\n" +
                signedHeaders + "\n" +
                payloadHash;

        // Create string to sign
        String credentialScope = dateStamp + "/" + region + "/" + SERVICE + "/aws4_request";
        String stringToSign = ALGORITHM + "\n" +
                amzDate + "\n" +
                credentialScope + "\n" +
                sha256Hex(canonicalRequest);

        // Calculate signature
        byte[] signingKey = getSignatureKey(secretKey, dateStamp, region, SERVICE);
        String signature = hmacSha256Hex(signingKey, stringToSign);

        // Create authorization header
        String authorization = ALGORITHM + " " +
                "Credential=" + accessKey + "/" + credentialScope + ", " +
                "SignedHeaders=" + signedHeaders + ", " +
                "Signature=" + signature;

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-amz-date", amzDate);
        headers.set("Authorization", authorization);
        headers.set("x-amz-content-sha256", payloadHash);

        return headers;
    }

    private byte[] getSignatureKey(String key, String dateStamp, String regionName, String serviceName) throws Exception {
        byte[] kSecret = ("AWS4" + key).getBytes(StandardCharsets.UTF_8);
        byte[] kDate = hmacSha256(kSecret, dateStamp);
        byte[] kRegion = hmacSha256(kDate, regionName);
        byte[] kService = hmacSha256(kRegion, serviceName);
        return hmacSha256(kService, "aws4_request");
    }

    private byte[] hmacSha256(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private String hmacSha256Hex(byte[] key, String data) throws Exception {
        return bytesToHex(hmacSha256(key, data));
    }

    private String sha256Hex(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
