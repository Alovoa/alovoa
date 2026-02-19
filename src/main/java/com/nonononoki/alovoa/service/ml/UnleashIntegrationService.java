package com.nonononoki.alovoa.service.ml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class UnleashIntegrationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.aura.ml.unleash.enabled:false}")
    private boolean enabled;

    @Value("${app.aura.ml.unleash.url:http://localhost:4242/api}")
    private String unleashUrl;

    @Value("${app.aura.ml.unleash.api-token:}")
    private String apiToken;

    @Value("${app.aura.ml.unleash.app-name:aura-java-backend}")
    private String appName;

    public UnleashIntegrationService(RestTemplate restTemplate, ObjectMapper objectMapper) {
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

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
            if (apiToken != null && !apiToken.isBlank()) {
                headers.set("Authorization", apiToken);
            }

            ResponseEntity<String> response = restTemplate.exchange(
                    normalizedUrl() + "/admin/projects/default/features",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
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

    public boolean isFeatureEnabled(String featureName, Map<String, String> context) {
        if (!enabled || featureName == null || featureName.isBlank()) {
            return false;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (apiToken != null && !apiToken.isBlank()) {
                headers.set("Authorization", apiToken);
            }
            if (appName != null && !appName.isBlank()) {
                headers.set("UNLEASH-APPNAME", appName);
            }

            Map<String, Object> body = Map.of("context", context == null ? Map.of() : context);
            ResponseEntity<String> response = restTemplate.exchange(
                    normalizedUrl() + "/client/features",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return false;
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode features = root.path("features");
            if (features == null || !features.isArray()) {
                return false;
            }

            for (JsonNode feature : features) {
                String name = feature.path("name").asText("");
                boolean isEnabled = feature.path("enabled").asBoolean(false);
                if (featureName.equals(name) && isEnabled) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private String normalizedUrl() {
        if (unleashUrl == null || unleashUrl.isBlank()) {
            return "http://localhost:4242/api";
        }
        return unleashUrl.endsWith("/") ? unleashUrl.substring(0, unleashUrl.length() - 1) : unleashUrl;
    }
}

