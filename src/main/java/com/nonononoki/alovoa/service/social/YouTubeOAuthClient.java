package com.nonononoki.alovoa.service.social;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.model.AlovoaException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class YouTubeOAuthClient extends AbstractSocialOAuthClient {

    @Value("${app.social-connect.youtube.enabled:true}")
    private boolean enabled;

    @Value("${app.social-connect.youtube.client-id:}")
    private String clientId;

    @Value("${app.social-connect.youtube.client-secret:}")
    private String clientSecret;

    @Value("${app.social-connect.youtube.redirect-uri:${app.domain}/oauth2/connect/youtube/callback}")
    private String redirectUri;

    @Value("${app.social-connect.youtube.scope:openid profile email https://www.googleapis.com/auth/youtube.readonly}")
    private String scope;

    @Value("${app.social-connect.youtube.auth-url:https://accounts.google.com/o/oauth2/v2/auth}")
    private String authUrl;

    @Value("${app.social-connect.youtube.token-url:https://oauth2.googleapis.com/token}")
    private String tokenUrl;

    @Value("${app.social-connect.youtube.user-url:https://www.googleapis.com/oauth2/v3/userinfo}")
    private String userUrl;

    @Value("${app.social-connect.youtube.channel-url:https://www.googleapis.com/youtube/v3/channels?part=id,snippet&mine=true}")
    private String channelUrl;

    public YouTubeOAuthClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public String provider() {
        return "youtube";
    }

    @Override
    public boolean isConfigured() {
        return enabled && hasRequiredConfig(clientId, clientSecret, redirectUri);
    }

    @Override
    public String buildAuthorizationUrl(String state, String codeChallenge) {
        Map<String, String> params = linkedParams(
                "client_id", requiredConfig(clientId),
                "redirect_uri", requiredConfig(redirectUri),
                "response_type", "code",
                "scope", StringUtils.trimToEmpty(scope),
                "state", state,
                "access_type", "offline",
                "include_granted_scopes", "true",
                "prompt", "consent",
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
                "client_secret", requiredConfig(clientSecret),
                "redirect_uri", requiredConfig(redirectUri),
                "code_verifier", codeVerifier
        );
        return readToken(postForm(tokenUrl, tokenBody, null));
    }

    @Override
    public SocialProviderProfile fetchProfile(OAuthAccessToken accessToken) throws AlovoaException {
        String providerUserId = null;
        String username = null;
        String profileUrl = null;

        Map<String, Object> channelPayload = getJson(channelUrl, accessToken.getAccessToken());
        Object itemsObj = channelPayload.get("items");
        if (itemsObj instanceof List<?> items && !items.isEmpty() && items.get(0) instanceof Map<?, ?> firstRaw) {
            @SuppressWarnings("unchecked")
            Map<String, Object> first = (Map<String, Object>) firstRaw;
            providerUserId = asString(first.get("id"));
            String customUrl = asString(readPath(first, "snippet", "customUrl"));
            if (StringUtils.isNotBlank(customUrl)) {
                username = customUrl.startsWith("@") ? customUrl.substring(1) : customUrl;
            } else {
                username = asString(readPath(first, "snippet", "title"));
            }
            if (StringUtils.isNotBlank(providerUserId)) {
                profileUrl = "https://www.youtube.com/channel/" + providerUserId;
            }
        }

        if (providerUserId == null || username == null) {
            Map<String, Object> profile = getJson(userUrl, accessToken.getAccessToken());
            if (providerUserId == null) {
                providerUserId = asString(profile.get("sub"));
            }
            if (username == null) {
                username = asString(profile.get("name"));
                if (username == null) {
                    username = asString(profile.get("email"));
                }
            }
            if (profileUrl == null) {
                profileUrl = asString(profile.get("profile"));
            }
        }

        return new SocialProviderProfile(providerUserId, username, profileUrl);
    }
}
