package com.nonononoki.alovoa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

import java.time.LocalDateTime;
import java.util.List;

import com.nonononoki.alovoa.model.AlovoaException;
import org.apache.commons.lang3.StringUtils;
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

	@Value("${app.like.message.length}")
	private int likeMessageLength;

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
		// setup settings
		Mockito.when(authService.getCurrentUser()).thenReturn(user1);
		Mockito.when(authService.getCurrentUser(true)).thenReturn(user1);
		String img1 = Tools.imageToB64("img/profile1.png", imgMimePng);
		userService.updateProfilePicture(img1);
		userService.addInterest(INTEREST);
		userService.updateDescription("description1");
		userService.updateIntention(INTENTION_TEST);
		userService.updateMaxAge(100);
		userService.updateMinAge(16);
		userService.updatePreferedGender(2, true);

		Mockito.when(authService.getCurrentUser()).thenReturn(user2);
		Mockito.when(authService.getCurrentUser(true)).thenReturn(user2);
		String img2 = Tools.imageToB64("img/profile2.png", imgMimePng);
		userService.updateProfilePicture(img2);
		userService.addInterest(INTEREST);
		userService.updateDescription("description2");
		userService.updateIntention(INTENTION_TEST);
		userService.updateMaxAge(100);
		userService.updateMinAge(16);
		userService.updatePreferedGender(1, true);

		Mockito.when(authService.getCurrentUser()).thenReturn(user3);
		Mockito.when(authService.getCurrentUser(true)).thenReturn(user3);
		String img3 = Tools.imageToB64("img/profile3.png", imgMimePng);
		userService.updateProfilePicture(img3);
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

		userService.deleteInterest(authService.getCurrentUser().getInterests().get(0).getText());
		assertEquals(0, authService.getCurrentUser().getInterests().size());
		userService.addInterest(INTEREST);

		Mockito.when(authService.getCurrentUser()).thenReturn(user1);
		Mockito.when(authService.getCurrentUser(true)).thenReturn(user1);
		List<UserDto> searchDtos1 = searchService.search(0.0, 0.0, 50, 1).getUsers();
		assertEquals(2, searchDtos1.size());

		Mockito.when(authService.getCurrentUser()).thenReturn(user2);
		Mockito.when(authService.getCurrentUser(true)).thenReturn(user2);
		List<UserDto> searchDtos2 = searchService.search(0.0, 0.0, 50, 1).getUsers();
		assertEquals(1, searchDtos2.size());

		Mockito.when(authService.getCurrentUser()).thenReturn(user3);
		Mockito.when(authService.getCurrentUser(true)).thenReturn(user3);
		List<UserDto> searchDtos3 = searchService.search(0.0, 0.0, 50, 1).getUsers();
		assertEquals(1, searchDtos3.size());

		// Tip: 1 degree equals roughly 111km
		Mockito.when(authService.getCurrentUser()).thenReturn(user1);
		Mockito.when(authService.getCurrentUser(true)).thenReturn(user1);
		userService.updatePreferedGender(1, true);
		userService.updatePreferedGender(2, true);
		userService.updatePreferedGender(3, true);

		Mockito.when(authService.getCurrentUser()).thenReturn(user2);
		Mockito.when(authService.getCurrentUser(true)).thenReturn(user2);
		userService.updatePreferedGender(1, true);
		userService.updatePreferedGender(2, true);
		userService.updatePreferedGender(3, true);

		Mockito.when(authService.getCurrentUser()).thenReturn(user3);
		Mockito.when(authService.getCurrentUser(true)).thenReturn(user3);
		userService.updatePreferedGender(1, true);
		userService.updatePreferedGender(2, true);
		userService.updatePreferedGender(3, true);

		List<UserDto> searchDtos4 = searchService.search(0.0, 0.0, 50, 1).getUsers();
		assertEquals(2, searchDtos4.size());

		List<UserDto> searchDtos5 = searchService.search(0.45, 0.0, 50, 1).getUsers();
		assertEquals(2, searchDtos5.size());

		List<UserDto> searchDtos6 = searchService.search(0.46, 0.0, 50, 1).getUsers();
		assertEquals(2, searchDtos6.size());

		// search filtered by interest
		Mockito.when(authService.getCurrentUser()).thenReturn(user1);
		Mockito.when(authService.getCurrentUser(true)).thenReturn(user1);
		userService.deleteInterest(user1.getInterests().get(0).getText());
		Mockito.when(authService.getCurrentUser()).thenReturn(user3);
		Mockito.when(authService.getCurrentUser(true)).thenReturn(user3);
		List<UserDto> interestSearchDto1 = searchService.search(0.0, 0.0, 50, SearchService.SORT_INTEREST).getUsers();
		assertEquals(1, interestSearchDto1.size());
		Mockito.when(authService.getCurrentUser()).thenReturn(user1);
		Mockito.when(authService.getCurrentUser(true)).thenReturn(user1);
		userService.addInterest(INTEREST);

		// test preferedAge
		Mockito.when(authService.getCurrentUser()).thenReturn(user2);
		Mockito.when(authService.getCurrentUser(true)).thenReturn(user2);
		userService.updateMinAge(USER1_AGE + 1);
		List<UserDto> ageSearchDto1 = searchService.search(0.0, 0.0, 50, 1).getUsers();
		assertEquals(1, ageSearchDto1.size());
		userService.updateMaxAge(USER3_AGE - 1);
		List<UserDto> ageSearchDto2 = searchService.search(0.0, 0.0, 50, 1).getUsers();
		assertEquals(2, ageSearchDto2.size()); // TODO check for incompatible

		user2.getDates().setDateOfBirth(Tools.localDateToDate(LocalDateTime.now().minusYears(minAge).toLocalDate()));
		Mockito.when(authService.getCurrentUser()).thenReturn(user2);
		Mockito.when(authService.getCurrentUser(true)).thenReturn(user2);
		userService.updateMinAge(minAge);
		userService.updateMaxAge(maxAge);
		List<UserDto> ageSearchDto3 = searchService.search(0.0, 0.0, 50, 1).getUsers();
		assertEquals(0, ageSearchDto3.size());
		Mockito.when(authService.getCurrentUser()).thenReturn(user3);
		Mockito.when(authService.getCurrentUser(true)).thenReturn(user3);
		List<UserDto> ageSearchDto4 = searchService.search(0.0, 0.0, 50, 1).getUsers();
		assertEquals(1, ageSearchDto4.size());
		user2.getDates().setDateOfBirth(Tools.ageToDate(USER2_AGE));

		// likeUser
		Mockito.when(authService.getCurrentUser()).thenReturn(user3);
		Mockito.when(authService.getCurrentUser(true)).thenReturn(user3);
		String longMessage = StringUtils.repeat("*", likeMessageLength);
		assertThrows(AlovoaException.class, () -> {
			userService.likeUser(UserDto.encodeId(user1.getId(), textEncryptor), longMessage + "a");
		});
		userService.likeUser(UserDto.encodeId(user1.getId(), textEncryptor), StringUtils.repeat("*", likeMessageLength));
		assertEquals(1, userLikeRepo.count());
		assertEquals(1, user3.getLikes().size());
		assertEquals(1, userNotificationRepo.count());
		List<UserDto> searchDtos7 = searchService.search(0.0, 0.0, 50, 1).getUsers();
		assertEquals(1, searchDtos7.size());

		// hideUser
		userService.hideUser(UserDto.encodeId(user2.getId(), textEncryptor));
		assertEquals(1, userHideRepo.count());
		assertEquals(1, user3.getHiddenUsers().size());
		List<UserDto> searchDtos8 = searchService.search(0.0, 0.0, 50, 1).getUsers();
		assertEquals(0, searchDtos8.size());

		user3.getHiddenUsers().clear();
		userRepo.saveAndFlush(user3);
		assertEquals(0, userHideRepo.count());

		// blockUser
		userService.blockUser(UserDto.encodeId(user2.getId(), textEncryptor));
		assertEquals(1, userBlockRepo.count());
		assertEquals(1, user3.getBlockedUsers().size());

		String user2IdEnc = UserDto.encodeId(user2.getId(), textEncryptor);
		assertThrows(Exception.class, () -> {
			// cannot like user when blocked
			userService.likeUser(user2IdEnc, null);
		});

		userService.unblockUser(UserDto.encodeId(user2.getId(), textEncryptor));
		assertEquals(0, userBlockRepo.count());

		// like back
		String user3IdEnc = UserDto.encodeId(user3.getId(), textEncryptor);
		assertThrows(Exception.class, () -> {
			userService.likeUser(user3IdEnc, null);
		});

		Mockito.when(authService.getCurrentUser()).thenReturn(user1);
		Mockito.when(authService.getCurrentUser(true)).thenReturn(user1);
		userService.likeUser(UserDto.encodeId(user3.getId(), textEncryptor), null);

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
		userService.blockUser(UserDto.encodeId(user3.getId(), textEncryptor));
		assertEquals(1, userBlockRepo.count());
		assertThrows(Exception.class, () -> {
			messageService.send(user3.getConversations().get(0).getId(), "Hello");
		});

		assertEquals(2, messageRepo.count());

		Mockito.when(authService.getCurrentUser()).thenReturn(user3);
		Mockito.when(authService.getCurrentUser(true)).thenReturn(user3);
		assertThrows(Exception.class, () -> {
			messageService.send(user1.getConversations().get(0).getId(), "Hello");
		});

		assertEquals(2, messageRepo.count());

		Mockito.when(authService.getCurrentUser()).thenReturn(user1);
		Mockito.when(authService.getCurrentUser(true)).thenReturn(user1);
		userService.unblockUser(UserDto.encodeId(user3.getId(), textEncryptor));
		assertEquals(0, userBlockRepo.count());

		messageService.send(user1.getConversations().get(0).getId(), verylongString);
		assertEquals(3, messageRepo.count());

		for (int i = 0; i < maxConvoMessages; i++) {
			messageService.send(user1.getConversations().get(0).getId(), verylongString);
		}
		assertEquals(maxConvoMessages, messageRepo.count());

		assertEquals(0, userReportRepo.count());
		userService.reportUser(UserDto.encodeId(user3.getId(), textEncryptor), "report");
		assertEquals(1, userReportRepo.count());
	}

}
