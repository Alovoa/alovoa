package com.nonononoki.alovoa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserSocialAccount;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.SocialMediaImageImportDto;
import com.nonononoki.alovoa.repo.UserSocialAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SocialMediaImportServiceTest {

    private RestTemplate restTemplate;
    private SocialMediaImportService socialMediaImportService;
    private MockRestServiceServer mockServer;
    private UserSocialAccountRepository socialAccountRepo;
    private User user;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        socialAccountRepo = mock(UserSocialAccountRepository.class);
        socialMediaImportService = new SocialMediaImportService(restTemplate, new ObjectMapper(), socialAccountRepo);
        ReflectionTestUtils.setField(socialMediaImportService, "socialImportEnabled", true);
        ReflectionTestUtils.setField(socialMediaImportService, "maxDownloadBytes", 5_000_000);
        ReflectionTestUtils.setField(socialMediaImportService, "requireLinkedOwnership", true);
        mockServer = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();

        user = new User("tester@example.com");
        user.setId(1L);
    }

    @Test
    void importImage_directMediaUrl_throwsWhenOwnershipCannotBeVerified() {
        SocialMediaImageImportDto dto = new SocialMediaImageImportDto();
        dto.setProvider("x");
        dto.setMediaUrl("https://pbs.twimg.com/media/sample.png");

        UserSocialAccount linked = new UserSocialAccount();
        linked.setProvider("x");
        linked.setProviderUserId("123");
        linked.setProviderUsername("myhandle");
        linked.setProfileUrl("https://x.com/myhandle");

        when(socialAccountRepo.findByUserAndProvider(user, "x")).thenReturn(Optional.of(linked));

        AlovoaException ex = assertThrows(AlovoaException.class, () -> socialMediaImportService.importImage(user, dto));
        assertEquals("social_media_import_ownership_unverified", ex.getMessage());
    }

    @Test
    void importImage_postUrl_worksWhenLinkedIdentityMatches() throws Exception {
        byte[] imageBytes = "png-bytes".getBytes();
        String postUrl = "https://x.com/myhandle/status/123456789";
        String mediaUrl = "https://pbs.twimg.com/media/sample.png";

        mockServer.expect(once(), requestTo(mediaUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(imageBytes, MediaType.IMAGE_PNG));

        SocialMediaImageImportDto dto = new SocialMediaImageImportDto();
        dto.setProvider("x");
        dto.setPostUrl(postUrl);
        dto.setMediaUrl(mediaUrl);

        UserSocialAccount linked = new UserSocialAccount();
        linked.setProvider("x");
        linked.setProviderUserId("123");
        linked.setProviderUsername("myhandle");
        linked.setProfileUrl("https://x.com/myhandle");
        when(socialAccountRepo.findByUserAndProvider(user, "x")).thenReturn(Optional.of(linked));

        SocialMediaImportService.ImportedImage importedImage = socialMediaImportService.importImage(user, dto);

        assertEquals("x", importedImage.getProvider());
        assertEquals(mediaUrl, importedImage.getSourceUrl());
        assertEquals("image/png", importedImage.getMimeType());
        assertArrayEquals(imageBytes, importedImage.getBytes());
        assertEquals(true, importedImage.isSourceVerified());
        mockServer.verify();
    }

    @Test
    void importImage_youtubePostUrl_usesOEmbedThumbnail() throws Exception {
        byte[] imageBytes = "jpg-bytes".getBytes();
        String postUrl = "https://www.youtube.com/watch?v=abc123";
        String oEmbedUrl = "https://www.youtube.com/oembed?url=https%3A%2F%2Fwww.youtube.com%2Fwatch%3Fv%3Dabc123&format=json";
        String thumbnailUrl = "https://i.ytimg.com/vi/abc123/hqdefault.jpg";

        mockServer.expect(once(), requestTo(oEmbedUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"thumbnail_url\":\"" + thumbnailUrl + "\"}", MediaType.APPLICATION_JSON));

        mockServer.expect(once(), requestTo(thumbnailUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(imageBytes, MediaType.IMAGE_JPEG));

        SocialMediaImageImportDto dto = new SocialMediaImageImportDto();
        dto.setPostUrl(postUrl);

        UserSocialAccount linked = new UserSocialAccount();
        linked.setProvider("youtube");
        linked.setProviderUserId("abc");
        linked.setProviderUsername("channel");
        linked.setProfileUrl("https://www.youtube.com/channel/channel");
        when(socialAccountRepo.findByUserAndProvider(user, "youtube")).thenReturn(Optional.of(linked));

        AlovoaException ex = assertThrows(AlovoaException.class, () -> socialMediaImportService.importImage(user, dto));
        assertEquals("social_media_import_ownership_unverified", ex.getMessage());
    }

    @Test
    void importImage_youtubePostUrl_ownerFromOembedMatchesLinkedAccount() throws Exception {
        byte[] imageBytes = "jpg-bytes".getBytes();
        String postUrl = "https://www.youtube.com/watch?v=abc123";
        String oEmbedUrl = "https://www.youtube.com/oembed?url=https%3A%2F%2Fwww.youtube.com%2Fwatch%3Fv%3Dabc123&format=json";
        String thumbnailUrl = "https://i.ytimg.com/vi/abc123/hqdefault.jpg";
        String authorUrl = "https://www.youtube.com/channel/UC123456";

        mockServer.expect(once(), requestTo(oEmbedUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"thumbnail_url\":\"" + thumbnailUrl + "\",\"author_url\":\"" + authorUrl + "\"}", MediaType.APPLICATION_JSON));

        mockServer.expect(once(), requestTo(thumbnailUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(imageBytes, MediaType.IMAGE_JPEG));

        UserSocialAccount linked = new UserSocialAccount();
        linked.setProvider("youtube");
        linked.setProviderUserId("UC123456");
        linked.setProviderUsername("mychannel");
        linked.setProfileUrl("https://www.youtube.com/channel/UC123456");
        when(socialAccountRepo.findByUserAndProvider(user, "youtube")).thenReturn(Optional.of(linked));

        SocialMediaImageImportDto dto = new SocialMediaImageImportDto();
        dto.setPostUrl(postUrl);

        SocialMediaImportService.ImportedImage importedImage = socialMediaImportService.importImage(user, dto);

        assertEquals("youtube", importedImage.getProvider());
        assertEquals(thumbnailUrl, importedImage.getSourceUrl());
        assertEquals("image/jpeg", importedImage.getMimeType());
        assertArrayEquals(imageBytes, importedImage.getBytes());
        assertEquals(true, importedImage.isSourceVerified());
        mockServer.verify();
    }

    @Test
    void importImage_unsupportedProvider_throws() {
        SocialMediaImageImportDto dto = new SocialMediaImageImportDto();
        dto.setProvider("unknown");
        dto.setMediaUrl("https://example.com/sample.png");

        AlovoaException exception = assertThrows(AlovoaException.class,
                () -> socialMediaImportService.importImage(user, dto));
        assertEquals("social_media_import_provider_unsupported", exception.getMessage());
    }

    @Test
    void importImage_requiresLinkedAccount_whenRestrictionEnabled() {
        SocialMediaImageImportDto dto = new SocialMediaImageImportDto();
        dto.setProvider("x");
        dto.setPostUrl("https://x.com/someone/status/123");
        dto.setMediaUrl("https://pbs.twimg.com/media/sample.png");

        when(socialAccountRepo.findByUserAndProvider(user, "x")).thenReturn(Optional.empty());

        AlovoaException ex = assertThrows(AlovoaException.class, () -> socialMediaImportService.importImage(user, dto));
        assertEquals("social_media_import_requires_linked_account", ex.getMessage());
    }
}
