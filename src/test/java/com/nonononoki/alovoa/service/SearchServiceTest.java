package com.nonononoki.alovoa.service;

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
public class SearchServiceTest {

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

	private final int INTENTION_TEST = 1;

	private final String INTEREST = "interest";

	private static int user1Age = 18;
	private static int user2Age = 20;
	private static int user3Age = 30;
	
	private List<User> testUsers;
	
	@BeforeEach
	public void before() throws Exception {
		testUsers = RegisterServiceTest.getTestUsers(captchaService, registerService, firstNameLengthMax, firstNameLengthMin);
	}
	
	@AfterEach
	public void after() throws Exception {
		RegisterServiceTest.deleteAllUsers(userService, authService, captchaService, conversationRepo, userRepo);
	}

	@Test
	public void test() throws Exception {

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
		Assert.assertTrue("profile_picture", user3.getProfilePicture() != null);
		userService.addInterest(INTEREST);
		Assert.assertTrue("interest", user3.getInterests().size() == 1);
		String description = "description3";
		userService.updateDescription(description);
		Assert.assertTrue("description", user3.getDescription().equals(description));
		userService.updateIntention(INTENTION_TEST);
		Assert.assertTrue("intention", user3.getIntention().getId() == INTENTION_TEST);
		userService.updateMaxAge(maxAge);
		Assert.assertTrue("max_age", user3.getPreferedMaxAge() == maxAge);

		userService.updateMinAge(minAge);
		Assert.assertTrue("min_age", user3.getPreferedMinAge() == minAge);
		userService.updatePreferedGender(1, true);
		Assert.assertTrue(user3.getPreferedGenders().size() == 1);
		userService.updatePreferedGender(2, true);
		Assert.assertTrue(user3.getPreferedGenders().size() == 2);
		userService.updatePreferedGender(2, false);
		Assert.assertTrue(user3.getPreferedGenders().size() == 1);

		userService.deleteInterest(authService.getCurrentUser().getInterests().get(0).getId());
		Assert.assertTrue("interest", authService.getCurrentUser().getInterests().size() == 0);
		userService.addInterest(INTEREST);
		
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

		// search filtered by interest
		Mockito.when(authService.getCurrentUser()).thenReturn(user1);
		userService.deleteInterest(user1.getInterests().get(0).getId());
		Mockito.when(authService.getCurrentUser()).thenReturn(user3);
		List<UserDto> interestSearchDto1 = searchService.search(0.0, 0.0, 50, SearchService.SORT_INTEREST);
		Assert.assertEquals(interestSearchDto1.size(), 1);
		Mockito.when(authService.getCurrentUser()).thenReturn(user1);
		userService.addInterest(INTEREST);

		// test preferedAge
		Mockito.when(authService.getCurrentUser()).thenReturn(user2);
		userService.updateMinAge(user1Age + 1);
		List<UserDto> ageSearchDto1 = searchService.search(0.0, 0.0, 50, 1);
		Assert.assertEquals(ageSearchDto1.size(), 1);
		userService.updateMaxAge(user3Age - 1);
		List<UserDto> ageSearchDto2 = searchService.search(0.0, 0.0, 50, 1);
		Assert.assertEquals(ageSearchDto2.size(), 0);

		user2.getDates().setDateOfBirth(Tools.localDateToDate(LocalDateTime.now().minusYears(minAge).toLocalDate()));
		Mockito.when(authService.getCurrentUser()).thenReturn(user2);
		userService.updateMinAge(minAge);
		userService.updateMaxAge(maxAge);
		List<UserDto> ageSearchDto3 = searchService.search(0.0, 0.0, 50, 1);
		Assert.assertEquals(ageSearchDto3.size(), 0);
		Mockito.when(authService.getCurrentUser()).thenReturn(user3);
		List<UserDto> ageSearchDto4 = searchService.search(0.0, 0.0, 50, 1);
		Assert.assertEquals(ageSearchDto4.size(), 1);
		user2.getDates().setDateOfBirth(Tools.ageToDate(user2Age));

		// likeUser
		Mockito.when(authService.getCurrentUser()).thenReturn(user3);
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
		
		String user2IdEnc = UserDto.encodeId(user2.getId(), textEncryptor);
		Assert.assertThrows(Exception.class, () -> {
			// cannot like user when blocked
			userService.likeUser(user2IdEnc);
		});

		userService.unblockUser(UserDto.encodeId(user2.getId(), textEncryptor));
		Assert.assertEquals(userBlockRepo.count(), 0);

		// like back
		String user3IdEnc = UserDto.encodeId(user3.getId(), textEncryptor);
		Assert.assertThrows(Exception.class, () -> {
			userService.likeUser(user3IdEnc);
		});

		Mockito.when(authService.getCurrentUser()).thenReturn(user1);
		userService.likeUser(UserDto.encodeId(user3.getId(), textEncryptor));

		Assert.assertEquals(userLikeRepo.count(), 2);
		Assert.assertEquals(userNotificationRepo.count(), 2);
		Assert.assertEquals(conversationRepo.count(), 1);
		Assert.assertEquals(user1.getConversations().size(), 1);

		messageService.send(user1.getConversations().get(0).getId(), "Hello");
		Assert.assertEquals(messageRepo.count(), 1);

		String verylongString = StringUtils.repeat("*", maxMessageSize);
		messageService.send(user1.getConversations().get(0).getId(), verylongString);

		Assert.assertEquals(messageRepo.count(), 2);

		Assert.assertThrows(Exception.class, () -> {
			messageService.send(user1.getConversations().get(0).getId(), verylongString + "a");
		});

		Assert.assertEquals(messageRepo.count(), 2);

		// test sending message to blocked users
		userService.blockUser(UserDto.encodeId(user3.getId(), textEncryptor));
		Assert.assertEquals(userBlockRepo.count(), 1);
		Assert.assertThrows(Exception.class, () -> {
			messageService.send(user3.getConversations().get(0).getId(), "Hello");
		});

		Assert.assertEquals(messageRepo.count(), 2);

		Mockito.when(authService.getCurrentUser()).thenReturn(user3);
		Assert.assertThrows(Exception.class, () -> {
			messageService.send(user1.getConversations().get(0).getId(), "Hello");
		});

		Assert.assertEquals(messageRepo.count(), 2);

		Mockito.when(authService.getCurrentUser()).thenReturn(user1);
		userService.unblockUser(UserDto.encodeId(user3.getId(), textEncryptor));
		Assert.assertEquals(userBlockRepo.count(), 0);

		messageService.send(user1.getConversations().get(0).getId(), verylongString);
		Assert.assertEquals(messageRepo.count(), 3);

		for (int i = 0; i < maxConvoMessages; i++) {
			messageService.send(user1.getConversations().get(0).getId(), verylongString);
		}
		Assert.assertEquals(messageRepo.count(), maxConvoMessages);

		Assert.assertEquals(userReportRepo.count(), 0);
		Captcha captchaReport = captchaService.generate();
		userService.reportUser(UserDto.encodeId(user3.getId(), textEncryptor), captchaReport.getId(),
				captchaReport.getText(), "report");
		Assert.assertEquals(userReportRepo.count(), 1);
		
	}
	
}
