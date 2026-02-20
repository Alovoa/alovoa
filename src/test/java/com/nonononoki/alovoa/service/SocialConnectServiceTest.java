package com.nonononoki.alovoa.service;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserSocialAccount;
import com.nonononoki.alovoa.entity.user.UserSocialLinkSession;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.SocialLinkSessionStatusDto;
import com.nonononoki.alovoa.model.SocialLinkStartResponseDto;
import com.nonononoki.alovoa.repo.UserSocialAccountRepository;
import com.nonononoki.alovoa.repo.UserSocialLinkSessionRepository;
import com.nonononoki.alovoa.service.social.OAuthAccessToken;
import com.nonononoki.alovoa.service.social.SocialOAuthClient;
import com.nonononoki.alovoa.service.social.SocialProviderProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SocialConnectServiceTest {

    @Mock
    private AuthService authService;
    @Mock
    private UserSocialLinkSessionRepository linkSessionRepo;
    @Mock
    private UserSocialAccountRepository socialAccountRepo;
    @Mock
    private SocialOAuthClient youtubeClient;

    private SocialConnectService service;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(youtubeClient.provider()).thenReturn("youtube");
        lenient().when(youtubeClient.isConfigured()).thenReturn(true);
        service = new SocialConnectService(
                authService,
                linkSessionRepo,
                socialAccountRepo,
                List.of(youtubeClient)
        );
        ReflectionTestUtils.setField(service, "socialConnectEnabled", true);
        ReflectionTestUtils.setField(service, "sessionExpiryMinutes", 15L);

        user = new User("user1@example.com");
        user.setId(1L);
        lenient().when(authService.getCurrentUser(true)).thenReturn(user);
    }

    @Test
    void startLink_createsPendingSessionAndReturnsAuthUrl() throws Exception {
        when(youtubeClient.buildAuthorizationUrl(any(), any())).thenReturn("https://auth.example");
        when(linkSessionRepo.save(any(UserSocialLinkSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SocialLinkStartResponseDto response = service.startLink("youtube");

        assertNotNull(response.getSessionId());
        assertEquals("youtube", response.getProvider());
        assertEquals("https://auth.example", response.getAuthorizationUrl());
        assertNotNull(response.getExpiresAt());

        ArgumentCaptor<UserSocialLinkSession> captor = ArgumentCaptor.forClass(UserSocialLinkSession.class);
        verify(linkSessionRepo).save(captor.capture());
        UserSocialLinkSession saved = captor.getValue();
        assertEquals("youtube", saved.getProvider());
        assertEquals(UserSocialLinkSession.LinkStatus.PENDING, saved.getStatus());
        assertNotNull(saved.getStateToken());
        assertNotNull(saved.getCodeVerifier());
    }

    @Test
    void completeLinkCallback_linksAccountWhenAvailable() throws Exception {
        UserSocialLinkSession session = new UserSocialLinkSession();
        session.setUuid(UUID.randomUUID());
        session.setUser(user);
        session.setProvider("youtube");
        session.setStateToken("state-1");
        session.setCodeVerifier("verifier");
        session.setStatus(UserSocialLinkSession.LinkStatus.PENDING);
        session.setExpiresAt(Date.from(Instant.now().plus(Duration.ofMinutes(5))));

        when(linkSessionRepo.findByProviderAndStateToken("youtube", "state-1"))
                .thenReturn(Optional.of(session));
        when(youtubeClient.exchangeCode(eq("oauth-code"), eq("verifier")))
                .thenReturn(new OAuthAccessToken("access", "refresh", 3600L, "bearer"));
        when(youtubeClient.fetchProfile(any()))
                .thenReturn(new SocialProviderProfile("provider-user-1", "alice", "https://youtube.com/@alice"));
        when(socialAccountRepo.findByProviderAndProviderUserId("youtube", "provider-user-1"))
                .thenReturn(Optional.empty());
        UserSocialAccount savedAccount = new UserSocialAccount();
        savedAccount.setId(10L);
        savedAccount.setUuid(UUID.randomUUID());
        savedAccount.setUser(user);
        savedAccount.setProvider("youtube");
        savedAccount.setProviderUserId("provider-user-1");
        savedAccount.setProviderUsername("alice");
        savedAccount.setProfileUrl("https://youtube.com/@alice");
        when(socialAccountRepo.findByUserAndProvider(user, "youtube"))
                .thenReturn(Optional.empty(), Optional.of(savedAccount));
        when(socialAccountRepo.save(any(UserSocialAccount.class)))
                .thenReturn(savedAccount);
        when(linkSessionRepo.save(any(UserSocialLinkSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SocialLinkSessionStatusDto status = service.completeLinkCallback(
                "youtube",
                "state-1",
                "oauth-code",
                null,
                null
        );

        assertEquals("LINKED", status.getStatus());
        assertNotNull(status.getLinkedAccount());
        assertEquals("provider-user-1", status.getLinkedAccount().getProviderUserId());
        verify(socialAccountRepo, times(1)).save(any(UserSocialAccount.class));
    }

    @Test
    void completeLinkCallback_marksFailedWhenProviderAccountIsOwnedByAnotherUser() throws Exception {
        UserSocialLinkSession session = new UserSocialLinkSession();
        session.setUuid(UUID.randomUUID());
        session.setUser(user);
        session.setProvider("youtube");
        session.setStateToken("state-2");
        session.setCodeVerifier("verifier");
        session.setStatus(UserSocialLinkSession.LinkStatus.PENDING);
        session.setExpiresAt(Date.from(Instant.now().plus(Duration.ofMinutes(5))));

        User otherUser = new User("other@example.com");
        otherUser.setId(2L);

        UserSocialAccount existing = new UserSocialAccount();
        existing.setId(22L);
        existing.setUser(otherUser);
        existing.setProvider("youtube");
        existing.setProviderUserId("provider-user-2");

        when(linkSessionRepo.findByProviderAndStateToken("youtube", "state-2"))
                .thenReturn(Optional.of(session));
        when(youtubeClient.exchangeCode(any(), any()))
                .thenReturn(new OAuthAccessToken("access", null, 3600L, "bearer"));
        when(youtubeClient.fetchProfile(any()))
                .thenReturn(new SocialProviderProfile("provider-user-2", "bob", null));
        when(socialAccountRepo.findByProviderAndProviderUserId("youtube", "provider-user-2"))
                .thenReturn(Optional.of(existing));
        when(linkSessionRepo.save(any(UserSocialLinkSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SocialLinkSessionStatusDto status = service.completeLinkCallback(
                "youtube",
                "state-2",
                "oauth-code",
                null,
                null
        );

        assertEquals("FAILED", status.getStatus());
        assertEquals("social_connect_account_already_linked", status.getErrorMessage());
        verify(socialAccountRepo, never()).save(any(UserSocialAccount.class));
    }

    @Test
    void getSessionStatus_rejectsDifferentUser() throws Exception {
        UserSocialLinkSession session = new UserSocialLinkSession();
        session.setUuid(UUID.randomUUID());
        session.setProvider("youtube");
        session.setStatus(UserSocialLinkSession.LinkStatus.PENDING);
        session.setExpiresAt(Date.from(Instant.now().plus(Duration.ofMinutes(5))));
        User otherUser = new User("other@example.com");
        otherUser.setId(2L);
        session.setUser(otherUser);

        when(linkSessionRepo.findByUuid(session.getUuid())).thenReturn(Optional.of(session));

        AlovoaException ex = assertThrows(AlovoaException.class, () -> service.getLinkSessionStatus(session.getUuid()));
        assertEquals("social_connect_session_forbidden", ex.getMessage());
    }
}
