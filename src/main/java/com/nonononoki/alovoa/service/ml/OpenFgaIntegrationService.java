package com.nonononoki.alovoa.service.ml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class OpenFgaIntegrationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.aura.ml.openfga.enabled:false}")
    private boolean enabled;

    @Value("${app.aura.ml.openfga.url:http://localhost:8080}")
    private String openFgaUrl;

    @Value("${app.aura.ml.openfga.store-id:}")
    private String storeId;

    @Value("${app.aura.ml.openfga.authz-model-id:}")
    private String authzModelId;

    @Value("${app.aura.ml.openfga.api-token:}")
    private String apiToken;

    public OpenFgaIntegrationService(RestTemplate restTemplate, ObjectMapper objectMapper) {
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
        if (storeId == null || storeId.isBlank()) {
            return new IntegrationHealth(false, "missing_store_id");
        }

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    normalizedUrl() + "/stores/" + storeId,
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

    public boolean checkAccess(String user, String relation, String object) {
        if (!enabled || storeId == null || storeId.isBlank()) {
            return false;
        }
        if (user == null || user.isBlank() || relation == null || relation.isBlank() || object == null || object.isBlank()) {
            return false;
        }

        try {
            HttpHeaders headers = headers();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> tuple = new HashMap<>();
            tuple.put("user", user);
            tuple.put("relation", relation);
            tuple.put("object", object);

            Map<String, Object> body = new HashMap<>();
            body.put("tuple_key", tuple);
            if (authzModelId != null && !authzModelId.isBlank()) {
                body.put("authorization_model_id", authzModelId);
            }

            ResponseEntity<String> response = restTemplate.exchange(
                    normalizedUrl() + "/stores/" + storeId + "/check",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return false;
            }
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("allowed").asBoolean(false);
        } catch (Exception e) {
            return false;
        }
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        if (apiToken != null && !apiToken.isBlank()) {
            headers.setBearerAuth(apiToken);
        }
        return headers;
    }

    private String normalizedUrl() {
        if (openFgaUrl == null || openFgaUrl.isBlank()) {
            return "http://localhost:8080";
        }
        return openFgaUrl.endsWith("/") ? openFgaUrl.substring(0, openFgaUrl.length() - 1) : openFgaUrl;
    }
}

