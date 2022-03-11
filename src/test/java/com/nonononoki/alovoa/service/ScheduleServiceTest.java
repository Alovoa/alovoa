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

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.entity.Captcha;
import com.nonononoki.alovoa.entity.Contact;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserDeleteToken;
import com.nonononoki.alovoa.entity.user.UserHide;
import com.nonononoki.alovoa.entity.user.UserPasswordToken;
import com.nonononoki.alovoa.model.PasswordResetDto;
import com.nonononoki.alovoa.repo.CaptchaRepository;
import com.nonononoki.alovoa.repo.ContactRepository;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.UserDeleteTokenRepository;
import com.nonononoki.alovoa.repo.UserHideRepository;
import com.nonononoki.alovoa.repo.UserPasswordTokenRepository;
import com.nonononoki.alovoa.repo.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ScheduleServiceTest {

	@Autowired
	private CaptchaRepository captchaRepo;

	@Autowired
	private ContactRepository contactRepo;

	@Autowired
	private UserHideRepository userHideRepo;

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private UserPasswordTokenRepository passwordTokenRepository;

	@Autowired
	private UserDeleteTokenRepository userDeleteTokenRepository;

	@Autowired
	private ConversationRepository conversationRepo;

	@Autowired
	private ScheduleService scheduleService;

	// @MockBean
	@Autowired
	private CaptchaService captchaService;

	@MockBean
	private AuthService authService;

	@MockBean
	private MailService mailService;

	@Value("${app.schedule.delay.captcha}")
	private long captchaDelay;

	@Value("${app.schedule.delay.hide}")
	private long hideDelay;

	@Value("${app.schedule.delay.password-reset}")
	private long passwordResetDelay;

	@Value("${app.schedule.delay.delete-account}")
	private long deleteAccountDelay;

	@Value("${app.schedule.delay.contact}")
	private long contactDelay;

	@Value("${app.first-name.length-max}")
	private int firstNameLengthMax;

	@Value("${app.first-name.length-min}")
	private int firstNameLengthMin;

	@Autowired
	private CaptchaRepository catchaRepo;

	@Autowired
	private RegisterService registerService;

	@Autowired
	private PasswordService passwordService;

	@Autowired
	private UserService userService;

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

		Date currentDate = new Date();
		long currentDateTime = currentDate.getTime();

		// CAPTCHA
		
		catchaRepo.deleteAll();
		
		Captcha captchaOld = generateCaptcha("hash1");
		captchaOld.setDate(new Date(currentDateTime - captchaDelay - 1));
		catchaRepo.saveAndFlush(captchaOld);

		Captcha captchaNew = generateCaptcha("hash2");
		captchaNew.setDate(new Date(currentDateTime - captchaDelay));
		catchaRepo.saveAndFlush(captchaNew);

		assertEquals(2, catchaRepo.count());
		scheduleService.cleanCaptcha(currentDate);
		assertEquals(1, catchaRepo.count());

		// CONTACT
		Contact contactOld = new Contact();
		contactOld.setEmail("test" + Tools.MAIL_TEST_DOMAIN);
		contactOld.setHidden(false);
		contactOld.setMessage("test message");
		contactOld.setDate(new Date(currentDateTime - contactDelay - 1));
		contactRepo.saveAndFlush(contactOld);

		Contact contactNew = new Contact();
		contactNew.setEmail("test" + Tools.MAIL_TEST_DOMAIN);
		contactNew.setHidden(false);
		contactNew.setMessage("test message");
		contactNew.setDate(new Date(currentDateTime - contactDelay));
		contactRepo.saveAndFlush(contactNew);

		assertEquals(2, contactRepo.count());
		scheduleService.cleanContact(currentDate);
		assertEquals(1, contactRepo.count());

		// USERHIDE
		User user1 = testUsers.get(1);
		User user2 = testUsers.get(2);

		UserHide hideOld = new UserHide();
		hideOld.setDate(new Date(currentDateTime - hideDelay - 1));
		hideOld.setUserFrom(user1);
		hideOld.setUserTo(user2);
		user1.getHiddenUsers().add(hideOld);
		userRepo.saveAndFlush(user1);

		UserHide hideNew = new UserHide();
		hideNew.setDate(new Date(currentDateTime - hideDelay));
		hideNew.setUserFrom(user2);
		hideNew.setUserTo(user1);
		user2.getHiddenUsers().add(hideNew);
		userRepo.saveAndFlush(user2);

		assertEquals(2, userHideRepo.count());
		scheduleService.cleanUserHide(currentDate);
		assertEquals(1, userHideRepo.count());

		// USERPASSWORDTOKEN
		Captcha captcha1 = captchaService.generate();
		PasswordResetDto pwResetDto1 = new PasswordResetDto();
		pwResetDto1.setEmail(user1.getEmail());
		pwResetDto1.setCaptchaId(captcha1.getId());
		pwResetDto1.setCaptchaText(captcha1.getText());
		UserPasswordToken passwordTokenOld = passwordService.resetPasword(pwResetDto1);
		passwordTokenOld.setDate(new Date(currentDateTime - passwordResetDelay - 1));
		user1.setPasswordToken(passwordTokenOld);
		userRepo.saveAndFlush(user1);

		Captcha captcha2 = captchaService.generate();
		PasswordResetDto pwResetDto2 = new PasswordResetDto();
		pwResetDto2.setEmail(user2.getEmail());
		pwResetDto2.setCaptchaId(captcha2.getId());
		pwResetDto2.setCaptchaText(captcha2.getText());
		UserPasswordToken passwordTokenNew = passwordService.resetPasword(pwResetDto2);
		passwordTokenNew.setDate(new Date(currentDateTime - passwordResetDelay));
		user2.setPasswordToken(passwordTokenNew);
		userRepo.saveAndFlush(user2);

		assertEquals(2, passwordTokenRepository.count());
		scheduleService.cleanUserPasswordToken(currentDate);
		assertEquals(1, passwordTokenRepository.count());

		// USERDELETETOKEN
		Mockito.when(authService.getCurrentUser()).thenReturn(user1);
		UserDeleteToken deleteTokenOld = userService.deleteAccountRequest();
		Mockito.when(authService.getCurrentUser()).thenReturn(user2);
		UserDeleteToken deleteTokenNew = userService.deleteAccountRequest();

		assertEquals(2, userDeleteTokenRepository.count());
		deleteTokenOld.setDate(new Date(currentDateTime - passwordResetDelay - 1));
		user1.setDeleteToken(deleteTokenNew);
		userRepo.saveAndFlush(user1);

		deleteTokenNew.setDate(new Date(currentDateTime - passwordResetDelay));
		user2.setDeleteToken(deleteTokenNew);
		userRepo.saveAndFlush(user2);

		scheduleService.cleanUserDeleteToken(currentDate);
		assertEquals(1, userDeleteTokenRepository.count());
	}

	private Captcha generateCaptcha(String hashCode) {
		Captcha captcha = new Captcha();
		captcha.setDate(new Date());
		captcha.setImage(null);
		captcha.setText("test");
		captcha.setHashCode(hashCode);
		captcha = captchaRepo.saveAndFlush(captcha);
		return captcha;
	}
}
