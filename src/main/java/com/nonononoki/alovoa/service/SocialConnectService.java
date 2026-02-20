package com.nonononoki.alovoa.service;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserSocialAccount;
import com.nonononoki.alovoa.entity.user.UserSocialLinkSession;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.SocialLinkSessionStatusDto;
import com.nonononoki.alovoa.model.SocialLinkStartResponseDto;
import com.nonononoki.alovoa.model.SocialLinkedAccountDto;
import com.nonononoki.alovoa.repo.UserSocialAccountRepository;
import com.nonononoki.alovoa.repo.UserSocialLinkSessionRepository;
import com.nonononoki.alovoa.service.social.OAuthAccessToken;
import com.nonononoki.alovoa.service.social.SocialOAuthClient;
import com.nonononoki.alovoa.service.social.SocialProviderProfile;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class SocialConnectService {

    private static final String ERR_DISABLED = "social_connect_disabled";
    private static final String ERR_PROVIDER_UNSUPPORTED = "social_connect_provider_unsupported";
    private static final String ERR_PROVIDER_NOT_CONFIGURED = "social_connect_provider_not_configured";
    private static final String ERR_INVALID_STATE = "social_connect_invalid_state";
    private static final String ERR_MISSING_CODE = "social_connect_missing_code";
    private static final String ERR_LINK_EXPIRED = "social_connect_link_expired";
    private static final String ERR_ALREADY_LINKED = "social_connect_account_already_linked";
    private static final String ERR_PROVIDER_USER_MISSING = "social_connect_provider_user_not_found";
    private static final String ERR_SESSION_FORBIDDEN = "social_connect_session_forbidden";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AuthService authService;
    private final UserSocialLinkSessionRepository linkSessionRepo;
    private final UserSocialAccountRepository socialAccountRepo;
    private final Map<String, SocialOAuthClient> oauthClients;

    @Value("${app.social-connect.enabled:true}")
    private boolean socialConnectEnabled;

    @Value("${app.social-connect.session-expiry-minutes:15}")
    private long sessionExpiryMinutes;

    public SocialConnectService(AuthService authService,
                                UserSocialLinkSessionRepository linkSessionRepo,
                                UserSocialAccountRepository socialAccountRepo,
                                List<SocialOAuthClient> oauthClients) {
        this.authService = authService;
        this.linkSessionRepo = linkSessionRepo;
        this.socialAccountRepo = socialAccountRepo;
        Map<String, SocialOAuthClient> byProvider = new HashMap<>();
        for (SocialOAuthClient client : oauthClients) {
            byProvider.put(client.provider(), client);
        }
        this.oauthClients = Collections.unmodifiableMap(byProvider);
    }

    @Transactional
    public SocialLinkStartResponseDto startLink(String providerInput) throws AlovoaException {
        ensureEnabled();
        User user = authService.getCurrentUser(true);
        String provider = normalizeProvider(providerInput);
        SocialOAuthClient client = getClient(provider);
        if (!client.isConfigured()) {
            throw new AlovoaException(ERR_PROVIDER_NOT_CONFIGURED);
        }

        String state = randomToken(24);
        String codeVerifier = randomToken(64);
        String codeChallenge = sha256Base64Url(codeVerifier);
        Instant now = Instant.now();

        UserSocialLinkSession session = new UserSocialLinkSession();
        session.setUuid(UUID.randomUUID());
        session.setUser(user);
        session.setProvider(provider);
        session.setStateToken(state);
        session.setCodeVerifier(codeVerifier);
        session.setStatus(UserSocialLinkSession.LinkStatus.PENDING);
        session.setStartedAt(Date.from(now));
        session.setExpiresAt(Date.from(now.plus(Duration.ofMinutes(sessionExpiryMinutes))));
        session = linkSessionRepo.save(session);

        String authorizationUrl = client.buildAuthorizationUrl(state, codeChallenge);
        return new SocialLinkStartResponseDto(
                session.getUuid(),
                provider,
                authorizationUrl,
                session.getExpiresAt()
        );
    }

    @Transactional
    public SocialLinkSessionStatusDto completeLinkCallback(String providerInput,
                                                           String state,
                                                           String code,
                                                           String error,
                                                           String errorDescription) throws AlovoaException {
        ensureEnabled();
        String provider = normalizeProvider(providerInput);
        if (StringUtils.isBlank(state)) {
            throw new AlovoaException(ERR_INVALID_STATE);
        }

        UserSocialLinkSession session = linkSessionRepo.findByProviderAndStateToken(provider, state.trim())
                .orElseThrow(() -> new AlovoaException(ERR_INVALID_STATE));

        updateExpiryIfNeeded(session);
        if (session.getStatus() == UserSocialLinkSession.LinkStatus.EXPIRED) {
            throw new AlovoaException(ERR_LINK_EXPIRED);
        }
        if (session.getStatus() != UserSocialLinkSession.LinkStatus.PENDING) {
            return toSessionStatusDto(session);
        }

        if (StringUtils.isNotBlank(error)) {
            String callbackError = StringUtils.left(
                    StringUtils.defaultIfBlank(errorDescription, error), 255);
            failSession(session, callbackError);
            return toSessionStatusDto(session);
        }

        if (StringUtils.isBlank(code)) {
            failSession(session, ERR_MISSING_CODE);
            return toSessionStatusDto(session);
        }

        SocialOAuthClient client = getClient(provider);
        if (!client.isConfigured()) {
            failSession(session, ERR_PROVIDER_NOT_CONFIGURED);
            return toSessionStatusDto(session);
        }

        OAuthAccessToken token;
        SocialProviderProfile profile;
        try {
            token = client.exchangeCode(code.trim(), session.getCodeVerifier());
            profile = client.fetchProfile(token);
        } catch (AlovoaException e) {
            failSession(session, e.getMessage());
            return toSessionStatusDto(session);
        }

        if (profile == null || StringUtils.isBlank(profile.getProviderUserId())) {
            failSession(session, ERR_PROVIDER_USER_MISSING);
            return toSessionStatusDto(session);
        }

        try {
            linkAccount(session, profile);
        } catch (AlovoaException e) {
            failSession(session, e.getMessage());
            return toSessionStatusDto(session);
        }
        return toSessionStatusDto(session);
    }

    public List<SocialLinkedAccountDto> listLinkedAccounts() throws AlovoaException {
        ensureEnabled();
        User user = authService.getCurrentUser(true);
        return socialAccountRepo.findByUserOrderByLinkedAtDesc(user)
                .stream()
                .map(this::toLinkedAccountDto)
                .toList();
    }

    @Transactional
    public void unlinkAccount(String providerInput) throws AlovoaException {
        ensureEnabled();
        User user = authService.getCurrentUser(true);
        String provider = normalizeProvider(providerInput);
        socialAccountRepo.deleteByUserAndProvider(user, provider);
    }

    @Transactional
    public SocialLinkSessionStatusDto getLinkSessionStatus(UUID sessionId) throws AlovoaException {
        ensureEnabled();
        if (sessionId == null) {
            throw new AlovoaException(ERR_INVALID_STATE);
        }
        User user = authService.getCurrentUser(true);
        UserSocialLinkSession session = linkSessionRepo.findByUuid(sessionId)
                .orElseThrow(() -> new AlovoaException(ERR_INVALID_STATE));
        if (!Objects.equals(session.getUser().getId(), user.getId())) {
            throw new AlovoaException(ERR_SESSION_FORBIDDEN);
        }
        updateExpiryIfNeeded(session);
        return toSessionStatusDto(session);
    }

    private void linkAccount(UserSocialLinkSession session, SocialProviderProfile profile) throws AlovoaException {
        String provider = session.getProvider();
        String providerUserId = profile.getProviderUserId().trim();
        User user = session.getUser();

        Optional<UserSocialAccount> byProviderUser = socialAccountRepo.findByProviderAndProviderUserId(provider, providerUserId);
        if (byProviderUser.isPresent() && !Objects.equals(byProviderUser.get().getUser().getId(), user.getId())) {
            throw new AlovoaException(ERR_ALREADY_LINKED);
        }

        UserSocialAccount account = socialAccountRepo.findByUserAndProvider(user, provider)
                .orElseGet(UserSocialAccount::new);

        if (account.getId() == null) {
            account.setUuid(UUID.randomUUID());
            account.setUser(user);
            account.setProvider(provider);
            account.setLinkedAt(new Date());
        }
        account.setProviderUserId(providerUserId);
        account.setProviderUsername(StringUtils.trimToNull(profile.getProviderUsername()));
        account.setProfileUrl(StringUtils.trimToNull(profile.getProfileUrl()));
        account.setUpdatedAt(new Date());

        try {
            socialAccountRepo.save(account);
        } catch (DataIntegrityViolationException e) {
            throw new AlovoaException(ERR_ALREADY_LINKED);
        }

        session.setStatus(UserSocialLinkSession.LinkStatus.LINKED);
        session.setErrorMessage(null);
        session.setProviderUserId(account.getProviderUserId());
        session.setProviderUsername(account.getProviderUsername());
        session.setCompletedAt(new Date());
        linkSessionRepo.save(session);
    }

    private void failSession(UserSocialLinkSession session, String error) {
        session.setStatus(UserSocialLinkSession.LinkStatus.FAILED);
        session.setErrorMessage(StringUtils.left(StringUtils.defaultIfBlank(error, "social_connect_failed"), 255));
        session.setCompletedAt(new Date());
        linkSessionRepo.save(session);
    }

    private void updateExpiryIfNeeded(UserSocialLinkSession session) {
        if (session.getStatus() == UserSocialLinkSession.LinkStatus.PENDING
                && session.getExpiresAt() != null
                && session.getExpiresAt().before(new Date())) {
            session.setStatus(UserSocialLinkSession.LinkStatus.EXPIRED);
            session.setErrorMessage(ERR_LINK_EXPIRED);
            session.setCompletedAt(new Date());
            linkSessionRepo.save(session);
        }
    }

    private SocialLinkSessionStatusDto toSessionStatusDto(UserSocialLinkSession session) {
        SocialLinkedAccountDto linked = null;
        if (session.getStatus() == UserSocialLinkSession.LinkStatus.LINKED) {
            linked = socialAccountRepo.findByUserAndProvider(session.getUser(), session.getProvider())
                    .filter(acc -> StringUtils.equals(acc.getProviderUserId(), session.getProviderUserId()))
                    .map(this::toLinkedAccountDto)
                    .orElse(null);
        }
        return new SocialLinkSessionStatusDto(
                session.getUuid(),
                session.getProvider(),
                session.getStatus().name(),
                session.getErrorMessage(),
                session.getExpiresAt(),
                session.getCompletedAt(),
                linked
        );
    }

    private SocialLinkedAccountDto toLinkedAccountDto(UserSocialAccount account) {
        return new SocialLinkedAccountDto(
                account.getUuid(),
                account.getProvider(),
                account.getProviderUserId(),
                account.getProviderUsername(),
                account.getProfileUrl(),
                account.getLinkedAt()
        );
    }

    private void ensureEnabled() throws AlovoaException {
        if (!socialConnectEnabled) {
            throw new AlovoaException(ERR_DISABLED);
        }
    }

    private SocialOAuthClient getClient(String provider) throws AlovoaException {
        SocialOAuthClient client = oauthClients.get(provider);
        if (client == null) {
            throw new AlovoaException(ERR_PROVIDER_UNSUPPORTED);
        }
        return client;
    }

    private String normalizeProvider(String providerInput) throws AlovoaException {
        String provider = StringUtils.trimToNull(providerInput);
        if (provider == null) {
            throw new AlovoaException(ERR_PROVIDER_UNSUPPORTED);
        }
        provider = provider.toLowerCase(Locale.ROOT);
        return switch (provider) {
            case "youtube", "google" -> "youtube";
            case "x", "twitter" -> "x";
            case "instagram" -> "instagram";
            case "tiktok" -> "tiktok";
            default -> throw new AlovoaException(ERR_PROVIDER_UNSUPPORTED);
        };
    }

    private static String randomToken(int bytes) {
        byte[] data = new byte[bytes];
        SECURE_RANDOM.nextBytes(data);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static String sha256Base64Url(String value) throws AlovoaException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new AlovoaException("social_connect_pkce_failed");
        }
    }
}
