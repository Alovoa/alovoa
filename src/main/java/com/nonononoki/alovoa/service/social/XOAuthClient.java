package com.nonononoki.alovoa.service.social;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.model.AlovoaException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Service
public class XOAuthClient extends AbstractSocialOAuthClient {

    @Value("${app.social-connect.x.enabled:true}")
    private boolean enabled;

    @Value("${app.social-connect.x.client-id:}")
    private String clientId;

    @Value("${app.social-connect.x.client-secret:}")
    private String clientSecret;

    @Value("${app.social-connect.x.redirect-uri:${app.domain}/oauth2/connect/x/callback}")
    private String redirectUri;

    @Value("${app.social-connect.x.scope:tweet.read users.read offline.access}")
    private String scope;

    @Value("${app.social-connect.x.auth-url:https://x.com/i/oauth2/authorize}")
    private String authUrl;

    @Value("${app.social-connect.x.token-url:https://api.x.com/2/oauth2/token}")
    private String tokenUrl;

    @Value("${app.social-connect.x.user-url:https://api.x.com/2/users/me?user.fields=id,name,username}")
    private String userUrl;

    public XOAuthClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public String provider() {
        return "x";
    }

    @Override
    public boolean isConfigured() {
        return enabled && hasRequiredConfig(clientId, redirectUri);
    }

    @Override
    public String buildAuthorizationUrl(String state, String codeChallenge) {
        Map<String, String> params = linkedParams(
                "response_type", "code",
                "client_id", requiredConfig(clientId),
                "redirect_uri", requiredConfig(redirectUri),
                "scope", StringUtils.trimToEmpty(scope),
                "state", state,
                "code_challenge", codeChallenge,
                "code_challenge_method", "S256"
        );
        return urlWithParams(authUrl, params);
    }

    @Override
    public OAuthAccessToken exchangeCode(String code, String codeVerifier) throws AlovoaException {
        Map<String, String> tokenBody = linkedParams(
                "grant_type", "authorization_code",
                "code", code,
                "client_id", requiredConfig(clientId),
                "redirect_uri", requiredConfig(redirectUri),
                "code_verifier", codeVerifier
        );
        HttpHeaders headers = new HttpHeaders();
        if (StringUtils.isNotBlank(clientSecret)) {
            String raw = clientId.trim() + ":" + clientSecret.trim();
            String encoded = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
            headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
        }
        return readToken(postForm(tokenUrl, tokenBody, headers));
    }

    @Override
    public SocialProviderProfile fetchProfile(OAuthAccessToken accessToken) throws AlovoaException {
        Map<String, Object> payload = getJson(userUrl, accessToken.getAccessToken());
        String providerUserId = asString(readPath(payload, "data", "id"));
        String username = asString(readPath(payload, "data", "username"));
        if (username == null) {
            username = asString(readPath(payload, "data", "name"));
        }
        String profileUrl = username == null ? null : "https://x.com/" + username;
        return new SocialProviderProfile(providerUserId, username, profileUrl);
    }
}
