package com.nonononoki.alovoa.service.social;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.model.AlovoaException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

abstract class AbstractSocialOAuthClient implements SocialOAuthClient {

    private static final String OAUTH_EXCHANGE_FAILED = "social_connect_token_exchange_failed";
    private static final String OAUTH_PROFILE_FAILED = "social_connect_profile_fetch_failed";

    protected final RestTemplate restTemplate;
    protected final ObjectMapper objectMapper;

    protected AbstractSocialOAuthClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    protected String urlWithParams(String baseUrl, Map<String, String> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (StringUtils.isNotBlank(entry.getValue())) {
                builder.queryParam(entry.getKey(), entry.getValue());
            }
        }
        return builder.build(true).toUriString();
    }

    protected Map<String, Object> postForm(String url, Map<String, String> form, HttpHeaders additionalHeaders)
            throws AlovoaException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (additionalHeaders != null) {
            headers.addAll(additionalHeaders);
        }
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.setAll(form);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );
            return parseJsonObject(response.getBody(), OAUTH_EXCHANGE_FAILED);
        } catch (RestClientException e) {
            throw new AlovoaException(OAUTH_EXCHANGE_FAILED);
        }
    }

    protected Map<String, Object> getJson(String url, String bearerToken) throws AlovoaException {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (StringUtils.isNotBlank(bearerToken)) {
            headers.setBearerAuth(bearerToken);
        }
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
            return parseJsonObject(response.getBody(), OAUTH_PROFILE_FAILED);
        } catch (RestClientException e) {
            throw new AlovoaException(OAUTH_PROFILE_FAILED);
        }
    }

    protected Map<String, Object> parseJsonObject(String body, String errorCode) throws AlovoaException {
        if (StringUtils.isBlank(body)) {
            throw new AlovoaException(errorCode);
        }
        try {
            return objectMapper.readValue(body, new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new AlovoaException(errorCode);
        }
    }

    protected String requiredConfig(String value) {
        return StringUtils.trimToEmpty(value);
    }

    protected boolean hasRequiredConfig(String... values) {
        for (String value : values) {
            if (StringUtils.isBlank(value)) {
                return false;
            }
        }
        return true;
    }

    protected String asString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return StringUtils.trimToNull(text);
        }
        return StringUtils.trimToNull(String.valueOf(value));
    }

    protected Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    protected Object readPath(Map<String, Object> payload, String... path) {
        Object current = payload;
        for (String key : path) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = ((Map<String, Object>) map).get(key);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    protected OAuthAccessToken readToken(Map<String, Object> payload) {
        String accessToken = asString(payload.get("access_token"));
        String refreshToken = asString(payload.get("refresh_token"));
        String tokenType = asString(payload.get("token_type"));
        Long expiresIn = asLong(payload.get("expires_in"));
        if (accessToken == null) {
            accessToken = asString(payload.get("accessToken"));
        }
        if (tokenType != null) {
            tokenType = tokenType.toLowerCase(Locale.ROOT);
        }
        return new OAuthAccessToken(accessToken, refreshToken, expiresIn, tokenType);
    }

    protected Map<String, String> linkedParams(String... kv) {
        Map<String, String> params = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            params.put(kv[i], kv[i + 1]);
        }
        return params;
    }
}
