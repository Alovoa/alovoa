package com.nonononoki.alovoa.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.UserDeleteAccountDto;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.service.*;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private MailService mailService;

    @MockitoBean
    private IntakeService intakeService;

    @MockitoBean
    private S3StorageService s3StorageService;

    @MockitoBean
    private SocialMediaImportService socialMediaImportService;

    @Value("${app.first-name.length-max}")
    private int firstNameLengthMax;

    @Value("${app.first-name.length-min}")
    private int firstNameLengthMin;

    private List<User> testUsers;

    @BeforeEach
    void before() throws Exception {
        Mockito.when(mailService.sendMail(any(String.class), any(String.class), any(String.class), any(String.class)))
                .thenReturn(true);
        Mockito.when(s3StorageService.uploadMedia(any(byte[].class), any(String.class), any(S3StorageService.S3MediaType.class)))
                .thenReturn("test-key-" + UUID.randomUUID());
        testUsers = RegisterServiceTest.getTestUsers(captchaService, registerService, firstNameLengthMax, firstNameLengthMin);
    }

    @AfterEach
    void after() throws Exception {
        RegisterServiceTest.deleteAllUsers(userService, authService, captchaService, conversationRepo, userRepo);
    }

    @Test
    @WithMockUser
    @DisplayName("POST /user/post - Simple session test")
    void testPost() throws Exception {
        mockMvc.perform(post("/user/post"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /user/update/description - Update user description")
    void testUpdateDescription() throws Exception {
        User user = testUsers.get(0);
        Mockito.doReturn(user).when(authService).getCurrentUser(true);

        String newDescription = "This is my updated bio";

        mockMvc.perform(post("/user/update/description")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(newDescription))
                .andExpect(status().isOk());

        // Verify description was updated
        User updatedUser = userRepo.findById(user.getId()).orElseThrow();
        assertEquals(newDescription, updatedUser.getDescription());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /user/update/intention/{intention} - Update intention")
    void testUpdateIntention() throws Exception {
        User user = testUsers.get(0);
        Mockito.doReturn(user).when(authService).getCurrentUser(true);

        mockMvc.perform(post("/user/update/intention/2"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /user/update/min-age/{minAge} - Update minimum age")
    void testUpdateMinAge() throws Exception {
        User user = testUsers.get(0);
        Mockito.doReturn(user).when(authService).getCurrentUser(true);

        mockMvc.perform(post("/user/update/min-age/25"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /user/update/max-age/{maxAge} - Update maximum age")
    void testUpdateMaxAge() throws Exception {
        User user = testUsers.get(0);
        Mockito.doReturn(user).when(authService).getCurrentUser(true);

        mockMvc.perform(post("/user/update/max-age/45"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /user/update/preferedGender/{genderId}/{activated} - Update preferred gender")
    void testUpdatePreferedGender() throws Exception {
        User user = testUsers.get(0);
        Mockito.doReturn(user).when(authService).getCurrentUser(true);

        mockMvc.perform(post("/user/update/preferedGender/1/1"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/user/update/preferedGender/1/0"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /user/update/misc-info/{infoValue}/{activated} - Update misc info")
    void testUpdateMiscInfo() throws Exception {
        User user = testUsers.get(0);
        Mockito.doReturn(user).when(authService).getCurrentUser(true);

        mockMvc.perform(post("/user/update/misc-info/1/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /user/interest/add/{value} - Add interest")
    void testAddInterest() throws Exception {
        User user = testUsers.get(0);
        Mockito.doReturn(user).when(authService).getCurrentUser(true);

        mockMvc.perform(post("/user/interest/add/hiking"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /user/interest/delete/{value} - Delete interest")
    void testDeleteInterest() throws Exception {
        User user = testUsers.get(0);
        Mockito.doReturn(user).when(authService).getCurrentUser(true);

        // First add an interest
        userService.addInterest("hiking");

        // Then delete it
        mockMvc.perform(post("/user/interest/delete/hiking"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /user/interest/autocomplete/{name} - Autocomplete interests")
    void testInterestAutocomplete() throws Exception {
        User user = testUsers.get(0);
        Mockito.doReturn(user).when(authService).getCurrentUser(true);

        mockMvc.perform(get("/user/interest/autocomplete/hik"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /user/show-zodiac/update/{showZodiac} - Update show zodiac setting")
    void testUpdateShowZodiac() throws Exception {
        User user = testUsers.get(0);
        Mockito.doReturn(user).when(authService).getCurrentUser(true);

        mockMvc.perform(post("/user/show-zodiac/update/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /user/units/update/{units} - Update units setting")
    void testUpdateUnits() throws Exception {
        User user = testUsers.get(0);
        Mockito.doReturn(user).when(authService).getCurrentUser(true);

        mockMvc.perform(post("/user/units/update/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /user/like/{uuid} - Like user")
    void testLikeUser() throws Exception {
        User user1 = testUsers.get(0);
        User user2 = testUsers.get(1);
        Mockito.doReturn(user1).when(authService).getCurrentUser(true);

        mockMvc.perform(post("/user/like/" + user2.getUuid()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /user/like/{uuid}/{message} - Like user with message")
    void testLikeUserWithMessage() throws Exception {
        User user1 = testUsers.get(0);
        User user2 = testUsers.get(1);
        Mockito.doReturn(user1).when(authService).getCurrentUser(true);

        mockMvc.perform(post("/user/like/" + user2.getUuid() + "/hello"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /user/hide/{uuid} - Hide user")
    void testHideUser() throws Exception {
        User user1 = testUsers.get(0);
        User user2 = testUsers.get(1);
        Mockito.doReturn(user1).when(authService).getCurrentUser(true);

        mockMvc.perform(post("/user/hide/" + user2.getUuid()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /user/block/{uuid} - Block user")
    void testBlockUser() throws Exception {
        User user1 = testUsers.get(0);
        User user2 = testUsers.get(1);
        Mockito.doReturn(user1).when(authService).getCurrentUser(true);

        mockMvc.perform(post("/user/block/" + user2.getUuid()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /user/unblock/{uuid} - Unblock user")
    void testUnblockUser() throws Exception {
        User user1 = testUsers.get(0);
        User user2 = testUsers.get(1);
        Mockito.doReturn(user1).when(authService).getCurrentUser(true);

        // First block the user
        userService.blockUser(user2.getUuid());

        // Then unblock
        mockMvc.perform(post("/user/unblock/" + user2.getUuid()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /user/report/{uuid} - Report user")
    void testReportUser() throws Exception {
        User user1 = testUsers.get(0);
        User user2 = testUsers.get(1);
        Mockito.doReturn(user1).when(authService).getCurrentUser(true);

        String reportComment = "Inappropriate behavior";

        mockMvc.perform(post("/user/report/" + user2.getUuid())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(reportComment))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /user/update/location/{latitude}/{longitude} - Update location")
    void testUpdateLocation() throws Exception {
        User user = testUsers.get(0);
        Mockito.doReturn(user).when(authService).getCurrentUser(true);

        mockMvc.perform(post("/user/update/location/38.9072/-77.0369"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /user/status/new-alert - Check for new alerts")
    void testNewAlert() throws Exception {
        User user = testUsers.get(0);
        Mockito.doReturn(user).when(authService).getCurrentUser(true);

        mockMvc.perform(get("/user/status/new-alert"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /user/status/new-alert/{lang} - Check for new alerts with language")
    void testNewAlertWithLang() throws Exception {
        User user = testUsers.get(0);
        Mockito.doReturn(user).when(authService).getCurrentUser(true);

        mockMvc.perform(get("/user/status/new-alert/en"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /user/status/new-message - Check for new messages")
    void testNewMessage() throws Exception {
        User user = testUsers.get(0);
        Mockito.doReturn(user).when(authService).getCurrentUser(true);

        mockMvc.perform(get("/user/status/new-message"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /user/reputation/{uuid} - Get user reputation")
    void testGetUserReputation() throws Exception {
        User user1 = testUsers.get(0);
        User user2 = testUsers.get(1);
        Mockito.doReturn(user1).when(authService).getCurrentUser(true);

        // Response format may vary - just verify endpoint returns OK
        mockMvc.perform(get("/user/reputation/" + user2.getUuid()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /user/profile/completeness - Get profile completeness")
    void testGetProfileCompleteness() throws Exception {
        User user = testUsers.get(0);
        Mockito.doReturn(user).when(authService).getCurrentUser(true);

        // Response format may vary - just verify endpoint returns OK
        mockMvc.perform(get("/user/profile/completeness"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /user/delete-account - Request account deletion")
    void testDeleteAccountRequest() throws Exception {
        User user = testUsers.get(0);
        Mockito.doReturn(user).when(authService).getCurrentUser(true);

        mockMvc.perform(post("/user/delete-account"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /user/image/delete/{imageId} - Delete image")
    void testDeleteImage() throws Exception {
        User user = testUsers.get(0);
        Mockito.doReturn(user).when(authService).getCurrentUser(true);

        // Deleting a non-existent image returns OK (idempotent behavior)
        mockMvc.perform(post("/user/image/delete/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /user/image/import-social - Import image from validated social media")
    void testImportSocialImage() throws Exception {
        User user = testUsers.get(0);
        Mockito.doReturn(user).when(authService).getCurrentUser(true);

        byte[] imageBytes = Tools.resourceToBytes("img/profile1.png");
        Mockito.when(socialMediaImportService.importImage(any(User.class), any(SocialMediaImageImportDto.class)))
                .thenReturn(new SocialMediaImportService.ImportedImage(
                        "tiktok",
                        "https://p16-sign-va.tiktokcdn.com/imported.png",
                        imageBytes,
                        "image/png",
                        true
                ));

        Map<String, Object> payload = new HashMap<>();
        payload.put("provider", "tiktok");
        payload.put("postUrl", "https://www.tiktok.com/@sample/video/123");

        mockMvc.perform(post("/user/image/import-social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /user/update/description - Empty description")
    void testUpdateDescription_Empty() throws Exception {
        User user = testUsers.get(0);
        Mockito.doReturn(user).when(authService).getCurrentUser(true);

        mockMvc.perform(post("/user/update/description")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(""))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /user/interest/add/{value} - Add multiple interests")
    void testAddMultipleInterests() throws Exception {
        User user = testUsers.get(0);
        Mockito.doReturn(user).when(authService).getCurrentUser(true);

        String[] interests = {"hiking", "reading", "cooking", "travel"};

        for (String interest : interests) {
            mockMvc.perform(post("/user/interest/add/" + interest))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @WithMockUser
    @DisplayName("GET /user/userdata/{uuid} - Get user data export (GDPR)")
    void testGetUserdata() throws Exception {
        User user = testUsers.get(0);
        Mockito.doReturn(user).when(authService).getCurrentUser(true);

        // This endpoint requires a specific UUID format from deletion token
        // AlovoaException is caught by global ExceptionHandler which returns 409 Conflict
        mockMvc.perform(get("/user/userdata/" + UUID.randomUUID()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /user/delete/audio - Delete audio")
    void testDeleteAudio() throws Exception {
        User user = testUsers.get(0);
        Mockito.doReturn(user).when(authService).getCurrentUser(true);

        mockMvc.perform(post("/user/delete/audio"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /user/get/audio/{uuid} - Get audio by UUID")
    void testGetAudio() throws Exception {
        User user = testUsers.get(0);
        Mockito.doReturn(user).when(authService).getCurrentUser(true);

        mockMvc.perform(get("/user/get/audio/" + user.getUuid()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /user/upvote/{uuid} and /user/downvote/{uuid} - Vote on verification")
    void testVoteVerificationPicture() throws Exception {
        User user1 = testUsers.get(0);
        User user2 = testUsers.get(1);
        Mockito.doReturn(user1).when(authService).getCurrentUser(true);

        // AlovoaException is caught by global ExceptionHandler which returns 409 Conflict
        mockMvc.perform(post("/user/update/verification-picture/upvote/" + user2.getUuid()))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/user/update/verification-picture/downvote/" + user2.getUuid()))
                .andExpect(status().isConflict());
    }
}
