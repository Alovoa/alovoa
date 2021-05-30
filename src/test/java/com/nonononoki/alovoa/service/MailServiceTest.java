package com.nonononoki.alovoa.service;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserDeleteToken;
import com.nonononoki.alovoa.entity.user.UserPasswordToken;
import com.nonononoki.alovoa.entity.user.UserRegisterToken;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MailServiceTest {

	@Autowired
	private RegisterService registerService;

	@Autowired
	private CaptchaService captchaService;

	@Value("${app.age.min}")
	private int minAge;
	
	@Value("${app.message.size}")
	private int maxMessageSize;
	
	@Value("${app.first-name.length-max}")
	private int firstNameLengthMax;

	@Value("${app.first-name.length-min}")
	private int firstNameLengthMin;

	@MockBean
	private AuthService authService;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private MailService mailService;

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private ConversationRepository conversationRepo;
	
	private List<User> testUsers;
	
	@BeforeEach
	void before() throws Exception {
		testUsers = RegisterServiceTest.getTestUsers(captchaService, registerService, firstNameLengthMax, firstNameLengthMin);
	}
	
	@AfterEach
	void after() throws Exception {
		RegisterServiceTest.deleteAllUsers(userService, authService, captchaService, conversationRepo, userRepo);
	}

	@Test
	void test() throws Exception {
		User user1 = testUsers.get(0);
		String subject = "test";
		String body = "test body";
		UserDeleteToken deleteToken = new UserDeleteToken();
		deleteToken.setContent("testToken");
		UserPasswordToken passwordToken = new UserPasswordToken();
		passwordToken.setContent("testToken");
		UserRegisterToken registerToken = new UserRegisterToken();
		registerToken.setContent("testToken");
		user1.setDeleteToken(deleteToken);
		user1.setPasswordToken(passwordToken);
		user1.setRegisterToken(registerToken);
		mailService.sendAdminMail(user1.getEmail(), subject, body);
		mailService.sendAccountConfirmed(user1);
		mailService.sendAccountDeleteConfirm(user1);
		mailService.sendAccountDeleteRequest(user1);
		mailService.sendPasswordResetMail(user1);
		mailService.sendRegistrationMail(user1);
	}
	
 }
