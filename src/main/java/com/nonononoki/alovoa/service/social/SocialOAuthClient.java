package com.nonononoki.alovoa.service.social;

import com.nonononoki.alovoa.model.AlovoaException;

public interface SocialOAuthClient {
    String provider();

    boolean isConfigured();

    String buildAuthorizationUrl(String state, String codeChallenge) throws AlovoaException;

    OAuthAccessToken exchangeCode(String code, String codeVerifier) throws AlovoaException;

    SocialProviderProfile fetchProfile(OAuthAccessToken accessToken) throws AlovoaException;
}
