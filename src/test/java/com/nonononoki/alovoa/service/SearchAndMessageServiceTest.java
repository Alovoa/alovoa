package com.nonononoki.alovoa.service;

import static org.mockito.ArgumentMatchers.any;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.Captcha;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.MessageRepository;
import com.nonononoki.alovoa.repo.UserBlockRepository;
import com.nonononoki.alovoa.repo.UserHideRepository;
import com.nonononoki.alovoa.repo.UserLikeRepository;
import com.nonononoki.alovoa.repo.UserNotificationRepository;
import com.nonononoki.alovoa.repo.UserReportRepository;
import com.nonononoki.alovoa.repo.UserRepository;

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

	@MockBean
	private AuthService authService;

	@MockBean
	private MailService mailService;

	private static final Long INTENTION_TEST = 1L;

	private final String INTEREST = "interest";

	private static final int USER1_AGE = 18;
	private static final int USER2_AGE = 20;
	private static final int USER3_AGE = 30;

	private List<User> testUsers;

	@BeforeEach
	void before() throws Exception {
		Mockito.doNothing().when(mailService).sendMail(Mockito.any(String.class), any(String.class), any(String.class),
				any(String.class));
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

		Assert.assertEquals(4, userRepo.count());

		String imgMimePng = "png";
		// setup settings
		Mockito.when(authService.getCurrentUser()).thenReturn(user1);
		String img1 = Tools.imageToB64("img/profile1.png", imgMimePng);
		userService.updateProfilePicture(img1);
		userService.addInterest(INTEREST);
		userService.updateDescription("description1");
		userService.updateIntention(INTENTION_TEST);
		userService.updateMaxAge(100);
		userService.updateMinAge(16);
		userService.updatePreferedGender(2, true);

		Mockito.when(authService.getCurrentUser()).thenReturn(user2);
		String img2 = Tools.imageToB64("img/profile2.png", imgMimePng);
		userService.updateProfilePicture(img2);
		userService.addInterest(INTEREST);
		userService.updateDescription("description2");
		userService.updateIntention(INTENTION_TEST);
		userService.updateMaxAge(100);
		userService.updateMinAge(16);
		userService.updatePreferedGender(1, true);

		Mockito.when(authService.getCurrentUser()).thenReturn(user3);
		String img3 = Tools.imageToB64("img/profile3.png", imgMimePng);
		userService.updateProfilePicture(img3);
		Assert.assertNotNull(user3.getProfilePicture());
		userService.addInterest(INTEREST);
		Assert.assertEquals(1, user3.getInterests().size());
		String description = "description3";
		userService.updateDescription(description);
		Assert.assertEquals(description, user3.getDescription());
		userService.updateIntention(INTENTION_TEST);
		Assert.assertEquals(INTENTION_TEST, user3.getIntention().getId());
		userService.updateMaxAge(maxAge);
		Assert.assertEquals(maxAge, user3.getPreferedMaxAge());

		userService.updateMinAge(minAge);
		Assert.assertEquals(minAge, user3.getPreferedMinAge());
		userService.updatePreferedGender(1, true);
		Assert.assertEquals(1, user3.getPreferedGenders().size());
		userService.updatePreferedGender(2, true);
		Assert.assertEquals(2, user3.getPreferedGenders().size());
		userService.updatePreferedGender(2, false);
		Assert.assertEquals(1, user3.getPreferedGenders().size());

		userService.deleteInterest(authService.getCurrentUser().getInterests().get(0).getId());
		Assert.assertEquals(0, authService.getCurrentUser().getInterests().size());
		userService.addInterest(INTEREST);

		Mockito.when(authService.getCurrentUser()).thenReturn(user1);
		List<UserDto> searchDtos1 = searchService.search(0.0, 0.0, 50, 1);
		Assert.assertEquals(2, searchDtos1.size());

		Mockito.when(authService.getCurrentUser()).thenReturn(user2);
		List<UserDto> searchDtos2 = searchService.search(0.0, 0.0, 50, 1);
		Assert.assertEquals(1, searchDtos2.size());

		Mockito.when(authService.getCurrentUser()).thenReturn(user3);
		List<UserDto> searchDtos3 = searchService.search(0.0, 0.0, 50, 1);
		Assert.assertEquals(1, searchDtos3.size());

		// Tip: 1 degree equals roughly 111km
		Mockito.when(authService.getCurrentUser()).thenReturn(user1);
		userService.updatePreferedGender(1, true);
		userService.updatePreferedGender(2, true);
		userService.updatePreferedGender(3, true);

		Mockito.when(authService.getCurrentUser()).thenReturn(user2);
		userService.updatePreferedGender(1, true);
		userService.updatePreferedGender(2, true);
		userService.updatePreferedGender(3, true);

		Mockito.when(authService.getCurrentUser()).thenReturn(user3);
		userService.updatePreferedGender(1, true);
		userService.updatePreferedGender(2, true);
		userService.updatePreferedGender(3, true);

		List<UserDto> searchDtos4 = searchService.search(0.0, 0.0, 50, 1);
		Assert.assertEquals(2, searchDtos4.size());

		List<UserDto> searchDtos5 = searchService.search(0.45, 0.0, 50, 1);
		Assert.assertEquals(2, searchDtos5.size());

		List<UserDto> searchDtos6 = searchService.search(0.46, 0.0, 50, 1);
		Assert.assertEquals(0, searchDtos6.size());

		// search filtered by interest
		Mockito.when(authService.getCurrentUser()).thenReturn(user1);
		userService.deleteInterest(user1.getInterests().get(0).getId());
		Mockito.when(authService.getCurrentUser()).thenReturn(user3);
		List<UserDto> interestSearchDto1 = searchService.search(0.0, 0.0, 50, SearchService.SORT_INTEREST);
		Assert.assertEquals(1, interestSearchDto1.size());
		Mockito.when(authService.getCurrentUser()).thenReturn(user1);
		userService.addInterest(INTEREST);

		// test preferedAge
		Mockito.when(authService.getCurrentUser()).thenReturn(user2);
		userService.updateMinAge(USER1_AGE + 1);
		List<UserDto> ageSearchDto1 = searchService.search(0.0, 0.0, 50, 1);
		Assert.assertEquals(1, ageSearchDto1.size());
		userService.updateMaxAge(USER3_AGE - 1);
		List<UserDto> ageSearchDto2 = searchService.search(0.0, 0.0, 50, 1);
		Assert.assertEquals(0, ageSearchDto2.size());

		user2.getDates().setDateOfBirth(Tools.localDateToDate(LocalDateTime.now().minusYears(minAge).toLocalDate()));
		Mockito.when(authService.getCurrentUser()).thenReturn(user2);
		userService.updateMinAge(minAge);
		userService.updateMaxAge(maxAge);
		List<UserDto> ageSearchDto3 = searchService.search(0.0, 0.0, 50, 1);
		Assert.assertEquals(0, ageSearchDto3.size());
		Mockito.when(authService.getCurrentUser()).thenReturn(user3);
		List<UserDto> ageSearchDto4 = searchService.search(0.0, 0.0, 50, 1);
		Assert.assertEquals(1, ageSearchDto4.size());
		user2.getDates().setDateOfBirth(Tools.ageToDate(USER2_AGE));

		// likeUser
		Mockito.when(authService.getCurrentUser()).thenReturn(user3);
		userService.likeUser(UserDto.encodeId(user1.getId(), textEncryptor));
		Assert.assertEquals(1, userLikeRepo.count());
		Assert.assertEquals(1, user3.getLikes().size());
		Assert.assertEquals(1, userNotificationRepo.count());
		List<UserDto> searchDtos7 = searchService.search(0.0, 0.0, 50, 1);
		Assert.assertEquals(1, searchDtos7.size());

		// hideUser
		userService.hideUser(UserDto.encodeId(user2.getId(), textEncryptor));
		Assert.assertEquals(1, userHideRepo.count());
		Assert.assertEquals(1, user3.getHiddenUsers().size());
		List<UserDto> searchDtos8 = searchService.search(0.0, 0.0, 50, 1);
		Assert.assertEquals(0, searchDtos8.size());

		user3.getHiddenUsers().clear();
		userRepo.saveAndFlush(user3);
		Assert.assertEquals(0, userHideRepo.count());

		// blockUser
		userService.blockUser(UserDto.encodeId(user2.getId(), textEncryptor));
		Assert.assertEquals(1, userBlockRepo.count());
		Assert.assertEquals(1, user3.getBlockedUsers().size());

		String user2IdEnc = UserDto.encodeId(user2.getId(), textEncryptor);
		Assert.assertThrows(Exception.class, () -> {
			// cannot like user when blocked
			userService.likeUser(user2IdEnc);
		});

		userService.unblockUser(UserDto.encodeId(user2.getId(), textEncryptor));
		Assert.assertEquals(0, userBlockRepo.count());

		// like back
		String user3IdEnc = UserDto.encodeId(user3.getId(), textEncryptor);
		Assert.assertThrows(Exception.class, () -> {
			userService.likeUser(user3IdEnc);
		});

		Mockito.when(authService.getCurrentUser()).thenReturn(user1);
		userService.likeUser(UserDto.encodeId(user3.getId(), textEncryptor));

		Assert.assertEquals(2, userLikeRepo.count());
		Assert.assertEquals(2, userNotificationRepo.count());
		Assert.assertEquals(1, conversationRepo.count());
		Assert.assertEquals(1, user1.getConversations().size());

		messageService.send(user1.getConversations().get(0).getId(), "Hello");
		Assert.assertEquals(1, messageRepo.count());

		String verylongString = StringUtils.repeat("*", maxMessageSize);
		messageService.send(user1.getConversations().get(0).getId(), verylongString);

		Assert.assertEquals(2, messageRepo.count());

		Assert.assertThrows(Exception.class, () -> {
			messageService.send(user1.getConversations().get(0).getId(), verylongString + "a");
		});

		Assert.assertEquals(2, messageRepo.count());

		// test sending message to blocked users
		userService.blockUser(UserDto.encodeId(user3.getId(), textEncryptor));
		Assert.assertEquals(1, userBlockRepo.count());
		Assert.assertThrows(Exception.class, () -> {
			messageService.send(user3.getConversations().get(0).getId(), "Hello");
		});

		Assert.assertEquals(2, messageRepo.count());

		Mockito.when(authService.getCurrentUser()).thenReturn(user3);
		Assert.assertThrows(Exception.class, () -> {
			messageService.send(user1.getConversations().get(0).getId(), "Hello");
		});

		Assert.assertEquals(2, messageRepo.count());

		Mockito.when(authService.getCurrentUser()).thenReturn(user1);
		userService.unblockUser(UserDto.encodeId(user3.getId(), textEncryptor));
		Assert.assertEquals(0, userBlockRepo.count());

		messageService.send(user1.getConversations().get(0).getId(), verylongString);
		Assert.assertEquals(3, messageRepo.count());

		for (int i = 0; i < maxConvoMessages; i++) {
			messageService.send(user1.getConversations().get(0).getId(), verylongString);
		}
		Assert.assertEquals(maxConvoMessages, messageRepo.count());

		Assert.assertEquals(0, userReportRepo.count());
		Captcha captchaReport = captchaService.generate();
		userService.reportUser(UserDto.encodeId(user3.getId(), textEncryptor), captchaReport.getId(),
				captchaReport.getText(), "report");
		Assert.assertEquals(1, userReportRepo.count());
	}

}
