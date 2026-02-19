package com.nonononoki.alovoa.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.ContentModerationEvent;
import com.nonononoki.alovoa.model.ModerationResult;
import com.nonononoki.alovoa.repo.ContentModerationEventRepository;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * Comprehensive tests for ContentModerationService covering:
 * - Text moderation (toxic content detection)
 * - Image moderation
 * - Moderation event logging
 * - Auto-flagging thresholds
 * - Keyword filtering fallback
 * - Perspective API integration
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = {
        "app.moderation.enabled=true",
        "app.moderation.toxicity-threshold=0.7",
        "app.moderation.insult-threshold=0.8",
        "app.moderation.profanity-threshold=0.9"
})
class ContentModerationServiceTest {

    @Autowired
    private ContentModerationService moderationService;

    @Autowired
    private ContentModerationEventRepository moderationEventRepo;

    @Autowired
    private RegisterService registerService;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private ConversationRepository conversationRepo;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private MailService mailService;

    @MockitoBean
    private RestTemplate restTemplate;

    @Value("${app.first-name.length-max}")
    private int firstNameLengthMax;

    @Value("${app.first-name.length-min}")
    private int firstNameLengthMin;

    private List<User> testUsers;

    @BeforeEach
    void before() throws Exception {
        Mockito.when(mailService.sendMail(Mockito.any(String.class), any(String.class), any(String.class),
                any(String.class))).thenReturn(true);
        testUsers = RegisterServiceTest.getTestUsers(captchaService, registerService, firstNameLengthMax,
                firstNameLengthMin);

        // Setup default keyword lists for testing
        setupKeywordLists();

        // Reset and inject mock RestTemplate into the service
        Mockito.reset(restTemplate);
        ReflectionTestUtils.setField(moderationService, "restTemplate", restTemplate);

        // Reset moderation settings to defaults for test isolation
        ReflectionTestUtils.setField(moderationService, "moderationEnabled", true);
        ReflectionTestUtils.setField(moderationService, "perspectiveApiKey", "");
        ReflectionTestUtils.setField(moderationService, "toxicityThreshold", 0.7);
        ReflectionTestUtils.setField(moderationService, "insultThreshold", 0.8);
        ReflectionTestUtils.setField(moderationService, "profanityThreshold", 0.9);
        ReflectionTestUtils.setField(moderationService, "textModerationServiceEnabled", false);
        ReflectionTestUtils.setField(moderationService, "textModerationServiceUrl", "http://localhost:8081");
        ReflectionTestUtils.setField(moderationService, "imageModerationEnabled", false);
        ReflectionTestUtils.setField(moderationService, "imageModerationThreshold", 0.6);
        ReflectionTestUtils.setField(moderationService, "mediaServiceUrl", "http://localhost:8001");
    }

    @AfterEach
    void after() throws Exception {
        RegisterServiceTest.deleteAllUsers(userService, authService, captchaService, conversationRepo, userRepo);
    }

    private void setupKeywordLists() {
        // Set up test blocked words
        Set<String> blockedWords = new HashSet<>();
        blockedWords.add("badword1");
        blockedWords.add("badword2");
        blockedWords.add("offensive");

        Set<String> warningWords = new HashSet<>();
        warningWords.add("caution");
        warningWords.add("watch");

        ReflectionTestUtils.setField(moderationService, "blockedWords", blockedWords);
        ReflectionTestUtils.setField(moderationService, "warningWords", warningWords);
        ReflectionTestUtils.setField(moderationService, "blockedPatterns", List.of());
    }

    // === Text Moderation Tests ===

    @Test
    void testModerateContent_CleanText_Allowed() throws Exception {
        String cleanText = "Hello, this is a nice message!";

        ModerationResult result = moderationService.moderateContent(cleanText);

        assertTrue(result.isAllowed());
        assertEquals(0.0, result.getToxicityScore());
        assertTrue(result.getFlaggedCategories().isEmpty());
    }

    @Test
    void testModerateContent_NullContent_Allowed() throws Exception {
        ModerationResult result = moderationService.moderateContent(null);

        assertTrue(result.isAllowed());
        assertEquals(0.0, result.getToxicityScore());
    }

    @Test
    void testModerateContent_EmptyContent_Allowed() throws Exception {
        ModerationResult result = moderationService.moderateContent("");
        assertTrue(result.isAllowed());

        ModerationResult result2 = moderationService.moderateContent("   ");
        assertTrue(result2.isAllowed());
    }

    @Test
    void testKeywordFilter_BlockedWord_Blocked() throws Exception {
        String textWithBlockedWord = "This contains badword1 in the message";

        ModerationResult result = moderationService.moderateContent(textWithBlockedWord);

        assertFalse(result.isAllowed());
        assertEquals(1.0, result.getToxicityScore());
        assertTrue(result.getFlaggedCategories().contains("BLOCKED_WORD"));
        assertEquals("Content contains prohibited language", result.getReason());
    }

    @Test
    void testKeywordFilter_BlockedWordCaseInsensitive_Blocked() throws Exception {
        String textWithBlockedWord = "This contains BADWORD1 in uppercase";

        ModerationResult result = moderationService.moderateContent(textWithBlockedWord);

        assertFalse(result.isAllowed());
        assertTrue(result.getFlaggedCategories().contains("BLOCKED_WORD"));
    }

    @Test
    void testKeywordFilter_MultipleBlockedWords_BlockedOnFirst() throws Exception {
        String textWithMultipleBlocked = "This has badword1 and badword2";

        ModerationResult result = moderationService.moderateContent(textWithMultipleBlocked);

        assertFalse(result.isAllowed());
        assertTrue(result.getFlaggedCategories().contains("BLOCKED_WORD"));
    }

    @Test
    void testKeywordFilter_WarningWord_Allowed() throws Exception {
        // Warning words log but don't block
        String textWithWarning = "Please caution when reading this";

        ModerationResult result = moderationService.moderateContent(textWithWarning);

        assertTrue(result.isAllowed());
    }

    // === Perspective API Integration Tests ===

    @Test
    void testPerspectiveAPI_HighToxicity_Blocked() throws Exception {
        // Set API key to enable Perspective API mode
        ReflectionTestUtils.setField(moderationService, "perspectiveApiKey", "test-api-key");

        String toxicContent = "This is toxic content";

        // Mock API response with high toxicity
        String mockResponse = """
                {
                  "attributeScores": {
                    "TOXICITY": {"summaryScore": {"value": 0.85}},
                    "INSULT": {"summaryScore": {"value": 0.3}},
                    "PROFANITY": {"summaryScore": {"value": 0.2}},
                    "THREAT": {"summaryScore": {"value": 0.1}},
                    "IDENTITY_ATTACK": {"summaryScore": {"value": 0.1}}
                  }
                }
                """;

        Mockito.doReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK))
                .when(restTemplate).exchange(
                        anyString(),
                        any(HttpMethod.class),
                        any(HttpEntity.class),
                        eq(String.class)
                );

        ModerationResult result = moderationService.moderateContent(toxicContent);

        assertFalse(result.isAllowed());
        assertEquals(0.85, result.getToxicityScore());
        assertTrue(result.getFlaggedCategories().contains("TOXICITY"));
        assertTrue(result.getReason().contains("TOXICITY"));
    }

    @Test
    void testPerspectiveAPI_HighInsult_Blocked() throws Exception {
        ReflectionTestUtils.setField(moderationService, "perspectiveApiKey", "test-api-key");

        String insultContent = "Insulting message";

        String mockResponse = """
                {
                  "attributeScores": {
                    "TOXICITY": {"summaryScore": {"value": 0.5}},
                    "INSULT": {"summaryScore": {"value": 0.85}},
                    "PROFANITY": {"summaryScore": {"value": 0.2}},
                    "THREAT": {"summaryScore": {"value": 0.1}},
                    "IDENTITY_ATTACK": {"summaryScore": {"value": 0.1}}
                  }
                }
                """;

        Mockito.doReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK))
                .when(restTemplate).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class));

        ModerationResult result = moderationService.moderateContent(insultContent);

        assertFalse(result.isAllowed());
        assertTrue(result.getFlaggedCategories().contains("INSULT"));
    }

    @Test
    void testPerspectiveAPI_HighProfanity_Blocked() throws Exception {
        ReflectionTestUtils.setField(moderationService, "perspectiveApiKey", "test-api-key");

        String profaneContent = "Profane language here";

        String mockResponse = """
                {
                  "attributeScores": {
                    "TOXICITY": {"summaryScore": {"value": 0.5}},
                    "INSULT": {"summaryScore": {"value": 0.3}},
                    "PROFANITY": {"summaryScore": {"value": 0.95}},
                    "THREAT": {"summaryScore": {"value": 0.1}},
                    "IDENTITY_ATTACK": {"summaryScore": {"value": 0.1}}
                  }
                }
                """;

        Mockito.doReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK))
                .when(restTemplate).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class));

        ModerationResult result = moderationService.moderateContent(profaneContent);

        assertFalse(result.isAllowed());
        assertTrue(result.getFlaggedCategories().contains("PROFANITY"));
    }

    @Test
    void testPerspectiveAPI_MultipleViolations_Blocked() throws Exception {
        ReflectionTestUtils.setField(moderationService, "perspectiveApiKey", "test-api-key");

        String multiViolationContent = "Very bad content";

        String mockResponse = """
                {
                  "attributeScores": {
                    "TOXICITY": {"summaryScore": {"value": 0.85}},
                    "INSULT": {"summaryScore": {"value": 0.9}},
                    "PROFANITY": {"summaryScore": {"value": 0.95}},
                    "THREAT": {"summaryScore": {"value": 0.8}},
                    "IDENTITY_ATTACK": {"summaryScore": {"value": 0.75}}
                  }
                }
                """;

        Mockito.doReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK))
                .when(restTemplate).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class));

        ModerationResult result = moderationService.moderateContent(multiViolationContent);

        assertFalse(result.isAllowed());
        // Should flag multiple categories
        assertTrue(result.getFlaggedCategories().contains("TOXICITY"));
        assertTrue(result.getFlaggedCategories().contains("INSULT"));
        assertTrue(result.getFlaggedCategories().contains("PROFANITY"));
        assertTrue(result.getFlaggedCategories().contains("THREAT"));
        assertTrue(result.getFlaggedCategories().contains("IDENTITY_ATTACK"));
    }

    @Test
    void testPerspectiveAPI_BelowThreshold_Allowed() throws Exception {
        ReflectionTestUtils.setField(moderationService, "perspectiveApiKey", "test-api-key");

        String mildContent = "Slightly edgy but acceptable";

        String mockResponse = """
                {
                  "attributeScores": {
                    "TOXICITY": {"summaryScore": {"value": 0.4}},
                    "INSULT": {"summaryScore": {"value": 0.3}},
                    "PROFANITY": {"summaryScore": {"value": 0.2}},
                    "THREAT": {"summaryScore": {"value": 0.1}},
                    "IDENTITY_ATTACK": {"summaryScore": {"value": 0.1}}
                  }
                }
                """;

        Mockito.doReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK))
                .when(restTemplate).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class));

        ModerationResult result = moderationService.moderateContent(mildContent);

        assertTrue(result.isAllowed());
    }

    @Test
    void testPerspectiveAPI_APIFailure_FallsBackToKeywordFilter() throws Exception {
        ReflectionTestUtils.setField(moderationService, "perspectiveApiKey", "test-api-key");

        String content = "This has badword1";

        // Mock API failure
        Mockito.doThrow(new RuntimeException("API Error"))
                .when(restTemplate).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class));

        ModerationResult result = moderationService.moderateContent(content);

        // Should fallback to keyword filter and still block
        assertFalse(result.isAllowed());
        assertTrue(result.getFlaggedCategories().contains("BLOCKED_WORD"));
    }

    @Test
    void testTextModerationService_RemoteBlocked_LogsRemoteProvider() throws Exception {
        ReflectionTestUtils.setField(moderationService, "textModerationServiceEnabled", true);
        ReflectionTestUtils.setField(moderationService, "textModerationServiceUrl", "http://mock-moderation");

        User user = testUsers.get(0);
        String remoteResponse = """
                {
                  "is_allowed": false,
                  "decision": "BLOCK",
                  "toxicity_score": 0.93,
                  "blocked_categories": ["toxicity", "insult"],
                  "reason": "High toxicity",
                  "provider": "detoxify",
                  "model_version": "detoxify_multilingual",
                  "signals": {"toxicity": 0.93, "insult": 0.74}
                }
                """;

        Mockito.doReturn(new ResponseEntity<>(remoteResponse, HttpStatus.OK))
                .when(restTemplate).exchange(
                        eq("http://mock-moderation/moderation/text"),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        eq(String.class)
                );

        ModerationResult result = moderationService.moderateContent("problematic message", user, "MESSAGE");

        assertFalse(result.isAllowed());
        assertEquals(0.93, result.getToxicityScore());
        assertTrue(result.getFlaggedCategories().contains("TOXICITY"));
        assertTrue(result.getFlaggedCategories().contains("INSULT"));

        List<ContentModerationEvent> events = moderationEventRepo.findByUser(user);
        assertEquals(1, events.size());
        ContentModerationEvent event = events.get(0);
        assertEquals("detoxify", event.getProvider());
        assertEquals("detoxify_multilingual", event.getModelVersion());
        assertEquals("remote_text_service", event.getSourceMode());
        assertNotNull(event.getSignalJson());
        assertTrue(event.getSignalJson().contains("\"toxicity\""));

        Mockito.verify(restTemplate, Mockito.times(1))
                .exchange(
                        eq("http://mock-moderation/moderation/text"),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        eq(String.class)
                );
    }

    @Test
    void testTextModerationService_Non2xx_FallsBackToKeywordFilter() throws Exception {
        ReflectionTestUtils.setField(moderationService, "textModerationServiceEnabled", true);
        ReflectionTestUtils.setField(moderationService, "textModerationServiceUrl", "http://mock-moderation");

        User user = testUsers.get(0);

        Mockito.doReturn(new ResponseEntity<>("{\"error\":\"temporary\"}", HttpStatus.SERVICE_UNAVAILABLE))
                .when(restTemplate).exchange(
                        eq("http://mock-moderation/moderation/text"),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        eq(String.class)
                );

        ModerationResult result = moderationService.moderateContent("contains badword1", user, "MESSAGE");

        assertFalse(result.isAllowed());
        assertTrue(result.getFlaggedCategories().contains("BLOCKED_WORD"));

        List<ContentModerationEvent> events = moderationEventRepo.findByUser(user);
        assertEquals(1, events.size());
        ContentModerationEvent event = events.get(0);
        assertEquals("keyword_filter", event.getProvider());
        assertEquals("fallback", event.getSourceMode());
        assertNull(event.getModelVersion());
    }

    @Test
    void testTextModerationService_Exception_FallsBackToPerspective() throws Exception {
        ReflectionTestUtils.setField(moderationService, "textModerationServiceEnabled", true);
        ReflectionTestUtils.setField(moderationService, "textModerationServiceUrl", "http://mock-moderation");
        ReflectionTestUtils.setField(moderationService, "perspectiveApiKey", "test-api-key");

        User user = testUsers.get(0);

        String perspectiveResponse = """
                {
                  "attributeScores": {
                    "TOXICITY": {"summaryScore": {"value": 0.84}},
                    "INSULT": {"summaryScore": {"value": 0.30}},
                    "PROFANITY": {"summaryScore": {"value": 0.20}},
                    "THREAT": {"summaryScore": {"value": 0.10}},
                    "IDENTITY_ATTACK": {"summaryScore": {"value": 0.10}}
                  }
                }
                """;

        Mockito.doAnswer(invocation -> {
                    String url = invocation.getArgument(0, String.class);
                    if (url.contains("/moderation/text")) {
                        throw new RuntimeException("remote moderation unavailable");
                    }
                    if (url.contains("commentanalyzer.googleapis.com")) {
                        return new ResponseEntity<>(perspectiveResponse, HttpStatus.OK);
                    }
                    throw new IllegalArgumentException("Unexpected URL " + url);
                })
                .when(restTemplate).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class));

        ModerationResult result = moderationService.moderateContent("text routed to perspective", user, "MESSAGE");

        assertFalse(result.isAllowed());
        assertTrue(result.getFlaggedCategories().contains("TOXICITY"));

        List<ContentModerationEvent> events = moderationEventRepo.findByUser(user);
        assertEquals(1, events.size());
        ContentModerationEvent event = events.get(0);
        assertEquals("perspective_api", event.getProvider());
        assertEquals("api", event.getSourceMode());
    }

    // === Moderation Event Logging Tests ===

    @Test
    void testModerationEventLogging_BlockedContent() throws Exception {
        User user = testUsers.get(0);
        String contentType = "MESSAGE";
        String toxicContent = "Content with badword1";

        long initialCount = moderationEventRepo.count();

        ModerationResult result = moderationService.moderateContent(toxicContent, user, contentType);

        assertFalse(result.isAllowed());

        // Verify event was logged
        assertEquals(initialCount + 1, moderationEventRepo.count());

        List<ContentModerationEvent> events = moderationEventRepo.findByUser(user);
        assertEquals(1, events.size());

        ContentModerationEvent event = events.get(0);
        assertEquals(user.getId(), event.getUser().getId());
        assertEquals(contentType, event.getContentType());
        assertEquals(1.0, event.getToxicityScore());
        assertTrue(event.isBlocked());
        assertTrue(event.getFlaggedCategories().contains("BLOCKED_WORD"));
        assertNotNull(event.getCreatedAt());
    }

    @Test
    void testModerationEventLogging_AllowedContent() throws Exception {
        User user = testUsers.get(0);
        String contentType = "PROFILE";
        String cleanContent = "Nice clean content";

        long initialCount = moderationEventRepo.count();

        ModerationResult result = moderationService.moderateContent(cleanContent, user, contentType);

        assertTrue(result.isAllowed());

        // Event should still be logged
        assertEquals(initialCount + 1, moderationEventRepo.count());

        List<ContentModerationEvent> events = moderationEventRepo.findByUser(user);
        assertEquals(1, events.size());

        ContentModerationEvent event = events.get(0);
        assertFalse(event.isBlocked());
        assertEquals(0.0, event.getToxicityScore());
    }

    @Test
    void testModerationEventLogging_NoUserOrContentType() throws Exception {
        String content = "Some content";

        long initialCount = moderationEventRepo.count();

        // Should not log if user or contentType is null
        moderationService.moderateContent(content, null, null);
        assertEquals(initialCount, moderationEventRepo.count());

        moderationService.moderateContent(content, testUsers.get(0), null);
        assertEquals(initialCount, moderationEventRepo.count());

        moderationService.moderateContent(content, null, "MESSAGE");
        assertEquals(initialCount, moderationEventRepo.count());
    }

    @Test
    void testGetBlockedContentCount() throws Exception {
        User user1 = testUsers.get(0);
        User user2 = testUsers.get(1);

        // User 1 has 3 blocked events
        moderationService.moderateContent("badword1", user1, "MESSAGE");
        moderationService.moderateContent("badword2", user1, "MESSAGE");
        moderationService.moderateContent("offensive", user1, "PROFILE");

        // User 1 has 1 allowed event
        moderationService.moderateContent("clean content", user1, "MESSAGE");

        // User 2 has 1 blocked event
        moderationService.moderateContent("badword1", user2, "MESSAGE");

        assertEquals(3, moderationService.getBlockedContentCount(user1));
        assertEquals(1, moderationService.getBlockedContentCount(user2));
    }

    // === Configuration Tests ===

    @Test
    void testModeration_Disabled() throws Exception {
        ReflectionTestUtils.setField(moderationService, "moderationEnabled", false);

        String toxicContent = "badword1 badword2 offensive";

        ModerationResult result = moderationService.moderateContent(toxicContent);

        // Should allow everything when moderation is disabled
        assertTrue(result.isAllowed());
    }

    @Test
    void testThresholdConfiguration_CustomValues() throws Exception {
        ReflectionTestUtils.setField(moderationService, "perspectiveApiKey", "test-api-key");
        ReflectionTestUtils.setField(moderationService, "toxicityThreshold", 0.5);

        String content = "Content to moderate";

        String mockResponse = """
                {
                  "attributeScores": {
                    "TOXICITY": {"summaryScore": {"value": 0.6}},
                    "INSULT": {"summaryScore": {"value": 0.2}},
                    "PROFANITY": {"summaryScore": {"value": 0.1}},
                    "THREAT": {"summaryScore": {"value": 0.1}},
                    "IDENTITY_ATTACK": {"summaryScore": {"value": 0.1}}
                  }
                }
                """;

        Mockito.doReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK))
                .when(restTemplate).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class));

        ModerationResult result = moderationService.moderateContent(content);

        // Should be blocked with lower threshold
        assertFalse(result.isAllowed());
        assertTrue(result.getFlaggedCategories().contains("TOXICITY"));
    }

    // === Edge Cases ===

    @Test
    void testModerateContent_VeryLongText() throws Exception {
        String longText = "clean text ".repeat(1000);

        ModerationResult result = moderationService.moderateContent(longText);

        assertTrue(result.isAllowed());
    }

    @Test
    void testModerateContent_SpecialCharacters() throws Exception {
        String specialChars = "!@#$%^&*()_+-=[]{}|;:',.<>?/~`";

        ModerationResult result = moderationService.moderateContent(specialChars);

        assertTrue(result.isAllowed());
    }

    @Test
    void testModerateContent_Unicode() throws Exception {
        String unicode = "Hello 世界 🌍 Привет مرحبا";

        ModerationResult result = moderationService.moderateContent(unicode);

        assertTrue(result.isAllowed());
    }

    @Test
    void testModerateContent_MultipleContentTypes() throws Exception {
        User user = testUsers.get(0);

        moderationService.moderateContent("test", user, "MESSAGE");
        moderationService.moderateContent("test", user, "PROFILE");
        moderationService.moderateContent("test", user, "PROMPT_RESPONSE");

        List<ContentModerationEvent> events = moderationEventRepo.findByUser(user);
        assertEquals(3, events.size());

        Set<String> contentTypes = new HashSet<>();
        events.forEach(e -> contentTypes.add(e.getContentType()));
        assertTrue(contentTypes.contains("MESSAGE"));
        assertTrue(contentTypes.contains("PROFILE"));
        assertTrue(contentTypes.contains("PROMPT_RESPONSE"));
    }
}
