package com.nonononoki.alovoa.service.social;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.model.AlovoaException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class TikTokOAuthClient extends AbstractSocialOAuthClient {

    @Value("${app.social-connect.tiktok.enabled:true}")
    private boolean enabled;

    @Value("${app.social-connect.tiktok.client-id:}")
    private String clientId;

    @Value("${app.social-connect.tiktok.client-secret:}")
    private String clientSecret;

    @Value("${app.social-connect.tiktok.redirect-uri:${app.domain}/oauth2/connect/tiktok/callback}")
    private String redirectUri;

    @Value("${app.social-connect.tiktok.scope:user.info.basic}")
    private String scope;

    @Value("${app.social-connect.tiktok.auth-url:https://www.tiktok.com/v2/auth/authorize/}")
    private String authUrl;

    @Value("${app.social-connect.tiktok.token-url:https://open.tiktokapis.com/v2/oauth/token/}")
    private String tokenUrl;

    @Value("${app.social-connect.tiktok.user-url:https://open.tiktokapis.com/v2/user/info/?fields=open_id,display_name,username,avatar_url}")
    private String userUrl;

    public TikTokOAuthClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public String provider() {
        return "tiktok";
    }

    @Override
    public boolean isConfigured() {
        return enabled && hasRequiredConfig(clientId, clientSecret, redirectUri);
    }

    @Override
    public String buildAuthorizationUrl(String state, String codeChallenge) {
        Map<String, String> params = linkedParams(
                "client_key", requiredConfig(clientId),
                "redirect_uri", requiredConfig(redirectUri),
                "response_type", "code",
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
                "client_key", requiredConfig(clientId),
                "client_secret", requiredConfig(clientSecret),
                "code", code,
                "grant_type", "authorization_code",
                "redirect_uri", requiredConfig(redirectUri),
                "code_verifier", codeVerifier
        );
        return readToken(postForm(tokenUrl, tokenBody, null));
    }

    @Override
    public SocialProviderProfile fetchProfile(OAuthAccessToken accessToken) throws AlovoaException {
        Map<String, Object> payload = getJson(userUrl, accessToken.getAccessToken());
        String providerUserId = asString(readPath(payload, "data", "user", "open_id"));
        if (providerUserId == null) {
            providerUserId = asString(readPath(payload, "data", "open_id"));
        }
        String username = asString(readPath(payload, "data", "user", "username"));
        if (username == null) {
            username = asString(readPath(payload, "data", "user", "display_name"));
        }
        String profileUrl = username == null ? null : "https://www.tiktok.com/@" + username;
        return new SocialProviderProfile(providerUserId, username, profileUrl);
    }
}
