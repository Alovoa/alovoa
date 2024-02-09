package com.nonononoki.alovoa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import java.util.Date;
import java.util.List;

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

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserWebPush;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.repo.UserWebPushRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationServiceTest {

	@Autowired
	private RegisterService registerService;

	@Autowired
	private CaptchaService captchaService;

	@Value("${app.vapid.max}")
	private int vapidMax;

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

	@MockBean
	private MailService mailService;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private UserService userService;

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private ConversationRepository conversationRepo;

	@Autowired
	private UserWebPushRepository userWebPushRepository;

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

		User user1 = testUsers.get(1);

		Mockito.when(authService.getCurrentUser()).thenReturn(user1);
		Mockito.when(authService.getCurrentUser(true)).thenReturn(user1);

		Date oldDate = new Date();

		UserWebPush wp = new UserWebPush();
		wp.setDate(oldDate);
		for (int i = 0; i < vapidMax; i++) {
			notificationService.subscribe(wp);
		}
		assertEquals(vapidMax, userWebPushRepository.count());

		Date newDate = new Date();
		wp.setDate(newDate);
		notificationService.subscribe(wp);
		assertEquals(vapidMax, userWebPushRepository.count());

		user1 = userRepo.findByEmail(user1.getEmail());
		UserWebPush newWebPush = user1.getWebPush().get(vapidMax - 1);
		assertEquals(newDate, newWebPush.getDate());

	}

}
