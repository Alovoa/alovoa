package com.nonononoki.alovoa.service.social;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.model.AlovoaException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
public class InstagramOAuthClient extends AbstractSocialOAuthClient {

    @Value("${app.social-connect.instagram.enabled:true}")
    private boolean enabled;

    @Value("${app.social-connect.instagram.client-id:}")
    private String clientId;

    @Value("${app.social-connect.instagram.client-secret:}")
    private String clientSecret;

    @Value("${app.social-connect.instagram.redirect-uri:${app.domain}/oauth2/connect/instagram/callback}")
    private String redirectUri;

    @Value("${app.social-connect.instagram.scope:user_profile,user_media}")
    private String scope;

    @Value("${app.social-connect.instagram.auth-url:https://www.instagram.com/oauth/authorize}")
    private String authUrl;

    @Value("${app.social-connect.instagram.token-url:https://api.instagram.com/oauth/access_token}")
    private String tokenUrl;

    @Value("${app.social-connect.instagram.user-url:https://graph.instagram.com/me?fields=id,username}")
    private String userUrl;

    public InstagramOAuthClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public String provider() {
        return "instagram";
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
                "state", state
        );
        return urlWithParams(authUrl, params);
    }

    @Override
    public OAuthAccessToken exchangeCode(String code, String codeVerifier) throws AlovoaException {
        Map<String, String> tokenBody = linkedParams(
                "client_id", requiredConfig(clientId),
                "client_secret", requiredConfig(clientSecret),
                "grant_type", "authorization_code",
                "redirect_uri", requiredConfig(redirectUri),
                "code", code
        );
        return readToken(postForm(tokenUrl, tokenBody, null));
    }

    @Override
    public SocialProviderProfile fetchProfile(OAuthAccessToken accessToken) throws AlovoaException {
        String url = UriComponentsBuilder.fromUriString(userUrl)
                .queryParam("access_token", accessToken.getAccessToken())
                .build(true)
                .toUriString();
        Map<String, Object> payload = getJson(url, null);
        String providerUserId = asString(payload.get("id"));
        String username = asString(payload.get("username"));
        String profileUrl = username == null ? null : "https://instagram.com/" + username;
        return new SocialProviderProfile(providerUserId, username, profileUrl);
    }
}
