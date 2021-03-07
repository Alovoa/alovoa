package com.nonononoki.alovoa;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.Captcha;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserDeleteToken;
import com.nonononoki.alovoa.model.RegisterDto;
import com.nonononoki.alovoa.model.UserDeleteAccountDto;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.MessageRepository;
import com.nonononoki.alovoa.repo.UserBlockRepository;
import com.nonononoki.alovoa.repo.UserHideRepository;
import com.nonononoki.alovoa.repo.UserLikeRepository;
import com.nonononoki.alovoa.repo.UserNotificationRepository;
import com.nonononoki.alovoa.repo.UserReportRepository;
import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.service.AuthService;
import com.nonononoki.alovoa.service.CaptchaService;
import com.nonononoki.alovoa.service.MessageService;
import com.nonononoki.alovoa.service.RegisterService;
import com.nonononoki.alovoa.service.SearchService;
import com.nonononoki.alovoa.service.UserService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserTest {

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

	@Value("${app.message.size}")
	private int maxMessageSize;

	@MockBean
	private AuthService authService;

	private final int INTENTION_TEST = 1;

	private static List<User> testUsers = null;

	public static List<User> getTestUsers(CaptchaService captchaService, RegisterService registerService)
			throws Exception {
		if (testUsers == null) {
			// register and confirm test users
			Captcha c1 = captchaService.generate();
			RegisterDto user1Dto = createTestUserDto(1, c1, "test1");
			String tokenContent1 = registerService.register(user1Dto);
			User user1 = registerService.registerConfirm(tokenContent1);

			Captcha c2 = captchaService.generate();
			RegisterDto user2Dto = createTestUserDto(2, c2, "test2");
			String tokenContent2 = registerService.register(user2Dto);
			User user2 = registerService.registerConfirm(tokenContent2);

			Captcha c3 = captchaService.generate();
			RegisterDto user3Dto = createTestUserDto(2, c3, "test3");
			String tokenContent3 = registerService.register(user3Dto);
			User user3 = registerService.registerConfirm(tokenContent3);

			testUsers = new ArrayList<>();
			testUsers.add(user1);
			testUsers.add(user2);
			testUsers.add(user3);
		}

		return testUsers;
	}

	public static void deleteAllUsers(UserService userService, AuthService authService, CaptchaService captchaService,
			ConversationRepository conversationRepo, UserRepository userRepo) throws Exception {
		if (testUsers != null) {
			for (User user : testUsers) {
				if (!user.isAdmin()) {
					user = userRepo.findById(user.getId()).get();
					Mockito.when(authService.getCurrentUser()).thenReturn(user);
					UserDeleteToken token = userService.deleteAccountRequest();
					UserDeleteAccountDto dto = new UserDeleteAccountDto();
					Captcha captcha = captchaService.generate();
					dto.setCaptchaId(captcha.getId());
					dto.setCaptchaText(captcha.getText());
					dto.setConfirm(true);
					dto.setEmail(user.getEmail());
					dto.setTokenString(token.getContent());
					userService.deleteAccountConfirm(dto);
				}
			}
			testUsers = null;
		}
	}

	private static RegisterDto createTestUserDto(long gender, Captcha c, String email) throws IOException {
		final int AGE = 20;
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.YEAR, AGE * (-1));
		RegisterDto dto = new RegisterDto();
		dto.setEmail(email + Tools.MAIL_TEST_DOMAIN);
		dto.setDateOfBirth(calendar.getTime());
		dto.setPassword("test123");
		dto.setFirstName("test");
		dto.setGender(gender);
		if (c != null) {
			dto.setCaptchaId(c.getId());
			dto.setCaptchaText(c.getText());
		} else {
			dto.setCaptchaId(0);
			dto.setCaptchaText("test");
		}
		dto.setTermsConditions(true);
		dto.setPrivacy(true);
		return dto;
	}

	@Test
	public void test() throws Exception {

		// one default admin user
		Assert.assertEquals(userRepo.count(), 1);

		List<User> testUsers = getTestUsers(captchaService, registerService);
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

		Assert.assertEquals(userRepo.count(), 4);
		final String INTEREST = "interest";

		String imgMime = "png";
		// setup settings
		Mockito.when(authService.getCurrentUser()).thenReturn(user1);
		String img1 = Tools.imageToB64(Tools.getFileFromResources("img/profile1.png"), imgMime);
		userService.updateProfilePicture(img1);
		userService.addInterest(INTEREST);
		userService.updateDescription("description1");
		userService.updateIntention(INTENTION_TEST);
		userService.updateMaxAge(100);
		userService.updateMinAge(16);
		userService.updatePreferedGender(2, true);

		Mockito.when(authService.getCurrentUser()).thenReturn(user2);
		String img2 = Tools.imageToB64(Tools.getFileFromResources("img/profile2.png"), imgMime);
		userService.updateProfilePicture(img2);
		userService.addInterest(INTEREST);
		userService.updateDescription("description2");
		userService.updateIntention(INTENTION_TEST);
		userService.updateMaxAge(100);
		userService.updateMinAge(16);
		userService.updatePreferedGender(1, true);

		Mockito.when(authService.getCurrentUser()).thenReturn(user3);
		String img3 = Tools.imageToB64(Tools.getFileFromResources("img/profile3.png"), imgMime);
		userService.updateProfilePicture(img3);
		Assert.assertTrue("profile_picture", user3.getProfilePicture() != null);
		userService.addInterest(INTEREST);
		Assert.assertTrue("interest", user3.getInterests().size() == 1);
		String description = "description3";
		userService.updateDescription(description);
		Assert.assertTrue("description", user3.getDescription().equals(description));
		userService.updateIntention(INTENTION_TEST);
		Assert.assertTrue("intention", user3.getIntention().getId() == INTENTION_TEST);
		int maxAge = 100;
		userService.updateMaxAge(maxAge);
		Assert.assertTrue("max_age", user3.getPreferedMaxAge() == maxAge);
		int minAge = 16;
		userService.updateMinAge(minAge);
		Assert.assertTrue("min_age", user3.getPreferedMinAge() == minAge);
		userService.updatePreferedGender(1, true);
		// TODO Assert PREF_GENDER
		userService.deleteInterest(authService.getCurrentUser().getInterests().get(0).getId());
		Assert.assertTrue("interest", user3.getInterests().size() == 0);
		userService.addInterest(INTEREST);
		userService.addImage(img3);
		Assert.assertTrue("image", user3.getImages().size() == 1);
		userService.deleteImage(authService.getCurrentUser().getImages().get(0).getId());
		Assert.assertTrue("image", user3.getImages().size() == 0);
		userService.deleteProfilePicture();
		Assert.assertTrue("profile_picture", user3.getProfilePicture() == null);
		userService.updateProfilePicture(img3);
		Assert.assertTrue("profile_picture", user3.getProfilePicture() != null);
		Assert.assertThrows(Exception.class, () -> {
			userService.updateAudio(Tools.resourceToB64("audio/file_example_MP3_700KB.mp3"));
		});
		userService.updateAudio(Tools.resourceToB64("audio/file_example_MP3_470KB.mp3"));
		Assert.assertTrue("audio", user3.getAudio() != null);
		userService.deleteAudio();
		Assert.assertTrue("audio", user3.getAudio() == null);

		searchTest(user1, user2, user3);

		UserTest.deleteAllUsers(userService, authService, captchaService, conversationRepo, userRepo);

		Assert.assertEquals(conversationRepo.count(), 0);
		Assert.assertEquals(userRepo.count(), 1);

	}

	private void searchTest(User user1, User user2, User user3) throws Exception {

		Mockito.when(authService.getCurrentUser()).thenReturn(user1);
		List<UserDto> searchDtos1 = searchService.search(0.0, 0.0, 50, 1);
		Assert.assertEquals(searchDtos1.size(), 2);

		Mockito.when(authService.getCurrentUser()).thenReturn(user2);
		List<UserDto> searchDtos2 = searchService.search(0.0, 0.0, 50, 1);
		Assert.assertEquals(searchDtos2.size(), 1);

		Mockito.when(authService.getCurrentUser()).thenReturn(user3);
		List<UserDto> searchDtos3 = searchService.search(0.0, 0.0, 50, 1);
		Assert.assertEquals(searchDtos3.size(), 1);

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
		Assert.assertEquals(searchDtos4.size(), 2);

		List<UserDto> searchDtos5 = searchService.search(0.45, 0.0, 50, 1);
		Assert.assertEquals(searchDtos5.size(), 2);

		List<UserDto> searchDtos6 = searchService.search(0.46, 0.0, 50, 1);
		Assert.assertEquals(searchDtos6.size(), 0);

		// likeUser
		userService.likeUser(UserDto.encodeId(user1.getId(), textEncryptor));
		Assert.assertEquals(userLikeRepo.count(), 1);
		Assert.assertEquals(user3.getLikes().size(), 1);
		Assert.assertEquals(userNotificationRepo.count(), 1);
		List<UserDto> searchDtos7 = searchService.search(0.0, 0.0, 50, 1);
		Assert.assertEquals(searchDtos7.size(), 1);

		// hideUser
		userService.hideUser(UserDto.encodeId(user2.getId(), textEncryptor));
		Assert.assertEquals(userHideRepo.count(), 1);
		Assert.assertEquals(user3.getHiddenUsers().size(), 1);
		List<UserDto> searchDtos8 = searchService.search(0.0, 0.0, 50, 1);
		Assert.assertEquals(searchDtos8.size(), 0);

		user3.getHiddenUsers().clear();
		userRepo.saveAndFlush(user3);
		Assert.assertEquals(userHideRepo.count(), 0);

		// blockUser
		userService.blockUser(UserDto.encodeId(user2.getId(), textEncryptor));
		Assert.assertEquals(userBlockRepo.count(), 1);
		Assert.assertEquals(user3.getBlockedUsers().size(), 1);
		Assert.assertThrows(Exception.class, () -> {
			// cannot like user when blocked
			userService.likeUser(UserDto.encodeId(user2.getId(), textEncryptor));
		});

		userService.unblockUser(UserDto.encodeId(user2.getId(), textEncryptor));
		Assert.assertEquals(userBlockRepo.count(), 0);

		// like back
		Assert.assertThrows(Exception.class, () -> {
			userService.likeUser(UserDto.encodeId(user3.getId(), textEncryptor));
		});

		Mockito.when(authService.getCurrentUser()).thenReturn(user1);
		userService.likeUser(UserDto.encodeId(user3.getId(), textEncryptor));

		Assert.assertEquals(userLikeRepo.count(), 2);
		Assert.assertEquals(userNotificationRepo.count(), 2);
		Assert.assertEquals(conversationRepo.count(), 1);
		Assert.assertEquals(user1.getConversations().size(), 1);

		messageService.send(user1.getConversations().get(0), "Hello");
		Assert.assertEquals(messageRepo.count(), 1);

		String verylongString = StringUtils.repeat("*", maxMessageSize);
		messageService.send(user1.getConversations().get(0), verylongString);

		Assert.assertEquals(messageRepo.count(), 2);

		Assert.assertThrows(Exception.class, () -> {
			messageService.send(user1.getConversations().get(0), verylongString + "a");
		});

		Assert.assertEquals(messageRepo.count(), 2);

		// test sending message to blocked users
		userService.blockUser(UserDto.encodeId(user3.getId(), textEncryptor));
		Assert.assertEquals(userBlockRepo.count(), 1);
		Assert.assertThrows(Exception.class, () -> {
			messageService.send(user3.getConversations().get(0), "Hello");
		});

		Assert.assertEquals(messageRepo.count(), 2);

		Mockito.when(authService.getCurrentUser()).thenReturn(user3);
		Assert.assertThrows(Exception.class, () -> {
			messageService.send(user1.getConversations().get(0), "Hello");
		});

		Assert.assertEquals(messageRepo.count(), 2);

		Mockito.when(authService.getCurrentUser()).thenReturn(user1);
		userService.unblockUser(UserDto.encodeId(user3.getId(), textEncryptor));
		Assert.assertEquals(userBlockRepo.count(), 0);

		messageService.send(user1.getConversations().get(0), verylongString);
		Assert.assertEquals(messageRepo.count(), 3);

		Assert.assertEquals(userReportRepo.count(), 0);
		Captcha captchaReport = captchaService.generate();
		userService.reportUser(UserDto.encodeId(user3.getId(), textEncryptor), captchaReport.getId(),
				captchaReport.getText(), "report");
		Assert.assertEquals(userReportRepo.count(), 1);
	}
}
