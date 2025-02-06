package com.nonononoki.alovoa.service;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.Gender;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.SearchDto;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.repo.*;
import com.nonononoki.alovoa.util.AuthTestUtil;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SearchAndMessageServiceTest {

    @Autowired
    private RegisterService registerService;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private UserService userService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private UserLikeRepository userLikeRepo;

    @Autowired
    private UserHideRepository userHideRepo;

    @Autowired
    private UserBlockRepository userBlockRepo;

    @Autowired
    private UserReportRepository userReportRepo;

    @Autowired
    private UserNotificationRepository userNotificationRepo;

    @Autowired
    private ConversationRepository conversationRepo;

    @Autowired
    private MessageRepository messageRepo;

    @Autowired
    private TextEncryptorConverter textEncryptor;

    @Value("${app.age.min}")
    private int minAge;

    @Value("${app.age.max}")
    private int maxAge;

    @Value("${app.message.size}")
    private int maxMessageSize;

    @Value("${app.conversation.messages-max}")
    private int maxConvoMessages;

    @Value("${app.first-name.length-max}")
    private int firstNameLengthMax;

    @Value("${app.first-name.length-min}")
    private int firstNameLengthMin;

    @Value("${app.like.message.length}")
    private int likeMessageLength;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private MailService mailService;

    private static final Long INTENTION_TEST = 1L;

    private static final int USER1_AGE = 18;
    private static final int USER2_AGE = 20;
    private static final int USER3_AGE = 30;

    private List<User> testUsers;

    @BeforeEach
    void before() throws Exception {
        Mockito.when(mailService.sendMail(Mockito.any(String.class), any(String.class), any(String.class),
                any(String.class))).thenReturn(true);
        testUsers = RegisterServiceTest.getTestUsers(captchaService, registerService, firstNameLengthMax,
                firstNameLengthMin);
    }

    @AfterEach
    void after() throws Exception {
        RegisterServiceTest.deleteAllUsers(userService, authService, captchaService, conversationRepo, userRepo);
    }

    @Test
    void test() throws Exception {

        User user1 = testUsers.get(0);
        User user2 = testUsers.get(1);
        User user3 = testUsers.get(2);

        // set location manually since no extra service is needed
        user1.setLocationLatitude(0.0);
        user1.setLocationLongitude(0.0);

        user2.setLocationLatitude(0.0);
        user2.setLocationLongitude(0.0);

        user3.setLocationLatitude(0.0);
        user3.setLocationLongitude(0.0);

        userRepo.saveAndFlush(user1);
        userRepo.saveAndFlush(user2);
        userRepo.saveAndFlush(user3);

        assertEquals(4, userRepo.count());

        String imgMimePng = "png";
        String INTEREST = "interest";

        // setup settings
        AuthTestUtil.mockGetCurrentUser(authService, user1);
        byte[] img1 = Tools.resourceToBytes("img/profile1.png");
        userService.updateProfilePicture(img1, imgMimePng);
        userService.addInterest(INTEREST);
        userService.updateDescription("description1");
        userService.updateIntention(INTENTION_TEST);
        userService.updateMaxAge(maxAge);
        userService.updateMinAge(minAge);
        userService.updatePreferedGender(2, true);

        AuthTestUtil.mockGetCurrentUser(authService, user2);
        byte[] img2 = Tools.resourceToBytes("img/profile2.png");
        userService.updateProfilePicture(img2, imgMimePng);
        userService.addInterest(INTEREST);
        userService.updateDescription("description2");
        userService.updateIntention(INTENTION_TEST);
        userService.updateMaxAge(maxAge);
        userService.updateMinAge(minAge);
        userService.updatePreferedGender(1, true);

        AuthTestUtil.mockGetCurrentUser(authService, user3);
        byte[] img3 = Tools.resourceToBytes("img/profile3.png");
        userService.updateProfilePicture(img3, imgMimePng);
        assertNotNull(user3.getProfilePicture());
        userService.addInterest(INTEREST);
        assertEquals(1, user3.getInterests().size());
        String description = "description3";
        userService.updateDescription(description);
        assertEquals(description, user3.getDescription());
        userService.updateIntention(INTENTION_TEST);
        assertEquals(INTENTION_TEST, user3.getIntention().getId());
        userService.updateMaxAge(maxAge);
        assertEquals(maxAge, user3.getPreferedMaxAge());
        userService.updateMinAge(minAge);
        assertEquals(minAge, user3.getPreferedMinAge());

        userService.updatePreferedGender(1, true);
        assertEquals(3, user3.getPreferedGenders().size());
        userService.updatePreferedGender(2, true);
        assertEquals(3, user3.getPreferedGenders().size());
        userService.updatePreferedGender(2, false);
        assertEquals(2, user3.getPreferedGenders().size());

        AuthTestUtil.mockGetCurrentUser(authService, user1);
        List<UserDto> searchDtos1 = searchService.searchComplete(SearchService.SearchParams.builder().build()).getUsers();
        assertEquals(2, searchDtos1.size());

        AuthTestUtil.mockGetCurrentUser(authService, user2);
        List<UserDto> searchDtos2 = searchService.searchComplete(SearchService.SearchParams.builder().build()).getUsers();
        assertEquals(1, searchDtos2.size());

        AuthTestUtil.mockGetCurrentUser(authService, user3);
        List<UserDto> searchDtos3 = searchService.searchComplete(SearchService.SearchParams.builder().build()).getUsers();
        assertEquals(1, searchDtos3.size());

        AuthTestUtil.mockGetCurrentUser(authService, user3);
        userService.updatePreferedGender(2, true);
        assertEquals(3, user3.getPreferedGenders().size());
        List<UserDto> searchDtos4 = searchService.searchComplete(SearchService.SearchParams.builder().build()).getUsers();
        assertEquals(2, searchDtos4.size());

        // search filtered by interest
        AuthTestUtil.mockGetCurrentUser(authService, user1);
        List<UserDto> interestSearchDto2 = searchService.searchComplete(SearchService.SearchParams.builder().interests(Set.of(INTEREST)).showOutsideParameters(false).build()).getUsers();
        assertEquals(2, interestSearchDto2.size());
        List<UserDto> interestSearchDto = searchService.searchComplete(SearchService.SearchParams.builder().interests(Set.of(INTEREST + "DOESNOTEXIST")).showOutsideParameters(false).build()).getUsers();
        assertEquals(0, interestSearchDto.size());

        // search filtered by preferred gender
        AuthTestUtil.mockGetCurrentUser(authService, user1);
        List<UserDto> genderSearchDto = searchService.searchComplete(SearchService.SearchParams.builder().preferredGenderIds(Set.of(Gender.OTHER)).showOutsideParameters(false).build()).getUsers();
        assertEquals(0, interestSearchDto.size());
        assertEquals(1, user1.getPreferedGenders().size());
        List<UserDto> genderSearchDto2 = searchService.searchComplete(SearchService.SearchParams.builder().preferredGenderIds(Set.of(Gender.MALE, Gender.FEMALE, Gender.OTHER)).showOutsideParameters(false).build()).getUsers();
        assertEquals(2, interestSearchDto2.size());
        assertEquals(3, user1.getPreferedGenders().size());

        // search filtered by preferred age
        AuthTestUtil.mockGetCurrentUser(authService, user2);
        List<UserDto> ageSearchDto1 = searchService.searchComplete(SearchService.SearchParams.builder().preferredMinAge(USER1_AGE + 1).build()).getUsers();
        assertEquals(1, ageSearchDto1.size());
        assertEquals(USER1_AGE + 1, user2.getPreferedMinAge());
        SearchDto searchResult2 = searchService.searchComplete(SearchService.SearchParams.builder().preferredMaxAge(USER3_AGE - 1).build());
        List<UserDto> ageSearchDto2 = searchResult2.getUsers();
        assertEquals(0, ageSearchDto2.size());
        assertEquals(USER3_AGE - 1, user2.getPreferedMaxAge());
        user2.getDates().setDateOfBirth(Tools.localDateToDate(LocalDateTime.now().minusYears(minAge).toLocalDate()));
        AuthTestUtil.mockGetCurrentUser(authService, user2);
        List<UserDto> ageSearchDto3 = searchService.searchComplete(SearchService.SearchParams.builder().preferredMaxAge(maxAge).preferredMinAge(minAge).build()).getUsers();
        assertEquals(0, ageSearchDto3.size());
        AuthTestUtil.mockGetCurrentUser(authService, user3);
        List<UserDto> ageSearchDto4 = searchService.searchComplete(SearchService.SearchParams.builder().build()).getUsers();
        assertEquals(1, ageSearchDto4.size());
        user2.getDates().setDateOfBirth(Tools.ageToDate(USER2_AGE));

        // likeUser
        List<UserDto> searchDtos6 = searchService.searchComplete(SearchService.SearchParams.builder().build()).getUsers();
        AuthTestUtil.mockGetCurrentUser(authService, user3);
        assertEquals(2, searchDtos6.size());
        String longMessage = StringUtils.repeat("*", likeMessageLength);
        assertThrows(AlovoaException.class, () -> {
            userService.likeUser(user1.getUuid(), longMessage + "a");
        });
        userService.likeUser(user1.getUuid(), StringUtils.repeat("*", likeMessageLength));
        assertEquals(1, userLikeRepo.count());
        assertEquals(1, user3.getLikes().size());
        assertEquals(1, userNotificationRepo.count());
        List<UserDto> searchDtos7 = searchService.searchComplete(SearchService.SearchParams.builder().build()).getUsers();
        assertEquals(1, searchDtos7.size());

        // hideUser
        userService.hideUser(user2.getUuid());
        assertEquals(1, userHideRepo.count());
        assertEquals(1, user3.getHiddenUsers().size());
        List<UserDto> searchDtos8 = searchService.searchComplete(SearchService.SearchParams.builder().build()).getUsers();
        assertEquals(0, searchDtos8.size());

        user3.getHiddenUsers().clear();
        userRepo.saveAndFlush(user3);
        assertEquals(0, userHideRepo.count());

        // blockUser
        userService.blockUser(user2.getUuid());
        assertEquals(1, userBlockRepo.count());
        assertEquals(1, user3.getBlockedUsers().size());

        assertThrows(Exception.class, () -> {
            // cannot like user when blocked
            userService.likeUser(user2.getUuid(), null);
        });

        userService.unblockUser(user2.getUuid());
        assertEquals(0, userBlockRepo.count());

        // like back
        assertThrows(Exception.class, () -> {
            userService.likeUser(user3.getUuid(), null);
        });

        AuthTestUtil.mockGetCurrentUser(authService, user1);
        userService.likeUser(user3.getUuid(), null);

        assertEquals(2, userLikeRepo.count());
        assertEquals(2, userNotificationRepo.count());
        assertEquals(1, conversationRepo.count());
        assertEquals(1, user1.getConversations().size());

        messageService.send(user1.getConversations().get(0).getId(), "Hello");
        assertEquals(1, messageRepo.count());

        String verylongString = StringUtils.repeat("*", maxMessageSize);
        messageService.send(user1.getConversations().get(0).getId(), verylongString);

        assertEquals(2, messageRepo.count());

        assertThrows(Exception.class, () -> {
            messageService.send(user1.getConversations().get(0).getId(), verylongString + "a");
        });

        assertEquals(2, messageRepo.count());

        // test sending message to blocked users
        userService.blockUser(user3.getUuid());
        assertEquals(1, userBlockRepo.count());
        assertThrows(Exception.class, () -> {
            messageService.send(user3.getConversations().get(0).getId(), "Hello");
        });

        assertEquals(2, messageRepo.count());

        AuthTestUtil.mockGetCurrentUser(authService, user3);
        assertThrows(Exception.class, () -> {
            messageService.send(user1.getConversations().get(0).getId(), "Hello");
        });

        assertEquals(2, messageRepo.count());

        AuthTestUtil.mockGetCurrentUser(authService, user1);
        userService.unblockUser(user3.getUuid());
        assertEquals(0, userBlockRepo.count());

        messageService.send(user1.getConversations().get(0).getId(), verylongString);
        assertEquals(3, messageRepo.count());

        for (int i = 0; i < maxConvoMessages; i++) {
            messageService.send(user1.getConversations().get(0).getId(), verylongString);
        }
        assertEquals(maxConvoMessages, messageRepo.count());

        assertEquals(0, userReportRepo.count());
        userService.reportUser(user3.getUuid(), "report");
        assertEquals(1, userReportRepo.count());
    }

}
