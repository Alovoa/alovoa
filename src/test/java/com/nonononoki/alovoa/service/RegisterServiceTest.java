package com.nonononoki.alovoa.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
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

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.entity.Captcha;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserDeleteToken;
import com.nonononoki.alovoa.model.RegisterDto;
import com.nonononoki.alovoa.model.UserDeleteAccountDto;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.MessageRepository;
import com.nonononoki.alovoa.repo.UserBlockRepository;
import com.nonononoki.alovoa.repo.UserHideRepository;
import com.nonononoki.alovoa.repo.UserLikeRepository;
import com.nonononoki.alovoa.repo.UserReportRepository;
import com.nonononoki.alovoa.repo.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class RegisterServiceTest {

	@Autowired
	private CaptchaService captchaService;
	
	@Autowired
	private RegisterService registerService;

	@Autowired
	private UserService userService;

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
	private ConversationRepository conversationRepo;

	@Autowired
	private MessageRepository messageRepo;

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

	private static List<User> testUsers = null;

	private static int user1Age = 18;
	private static int user2Age = 20;
	private static int user3Age = 30;

	@Test
	public void test() throws Exception {

		// one default admin user
		Assert.assertEquals(userRepo.count(), 1);
		
		RegisterServiceTest.getTestUsers(captchaService, registerService, firstNameLengthMax,
				firstNameLengthMin);
		RegisterServiceTest.deleteAllUsers(userService, authService, captchaService, conversationRepo, userRepo);

		Assert.assertEquals(conversationRepo.count(), 0);
		Assert.assertEquals(messageRepo.count(), 0);
		Assert.assertEquals(userHideRepo.count(), 0);
		Assert.assertEquals(userReportRepo.count(), 0);
		Assert.assertEquals(userLikeRepo.count(), 0);
		Assert.assertEquals(userBlockRepo.count(), 0);
		Assert.assertEquals(userRepo.count(), 1);

	}

	public static List<User> getTestUsers(CaptchaService captchaService, RegisterService registerService,
			int firstNameLengthMin, int firstNameLengthMax) throws Exception {
		if (testUsers == null) {

			Captcha c2 = captchaService.generate();
			RegisterDto user2Dto = createTestUserDto(2, c2, "test2", user2Age);
			String tokenContent2 = registerService.register(user2Dto);
			User user2 = registerService.registerConfirm(tokenContent2);

			String user1Email = "nonononoki@gmail.com";
			// register and confirm test users
			Captcha c1 = captchaService.generate();
			RegisterDto user1Dto = createTestUserDto(1, c1, user1Email, user1Age);
			String tokenContent1 = registerService.register(user1Dto);
			User user1 = registerService.registerConfirm(tokenContent1);

			// test multiple email copies with slight variations
			{
				user1Dto.setFirstName(StringUtils.repeat("*", firstNameLengthMin - 1));
				Assert.assertThrows(Exception.class, () -> {
					registerService.register(user1Dto);
				});

				user1Dto.setFirstName(StringUtils.repeat("*", firstNameLengthMax + 1));
				Assert.assertThrows(Exception.class, () -> {
					registerService.register(user1Dto);
				});

				user1Dto.setFirstName("test");

				user1Dto.setEmail("nono.nono.ki@gmail.com");
				Assert.assertThrows(Exception.class, () -> {
					registerService.register(user1Dto);
				});

				user1Dto.setEmail("nonononoki+test@gmail.com");
				Assert.assertThrows(Exception.class, () -> {
					registerService.register(user1Dto);
				});

				user1Dto.setEmail("nono.nono.ki+test@gmail.com");
				Assert.assertThrows(Exception.class, () -> {
					registerService.register(user1Dto);
				});
			}

			Captcha c3 = captchaService.generate();
			RegisterDto user3Dto = createTestUserDto(2, c3, "test3", user3Age);
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
					token.setActiveDate(new Date());
					user.setDeleteToken(token);
					userRepo.saveAndFlush(user);
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

	private static RegisterDto createTestUserDto(long gender, Captcha c, String email, int age) throws IOException {
		RegisterDto dto = new RegisterDto();

		if (!email.contains("@")) {
			dto.setEmail(email + Tools.MAIL_TEST_DOMAIN);
		} else {
			dto.setEmail(email);
		}
		dto.setDateOfBirth(Tools.ageToDate(age));
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
}
