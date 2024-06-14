package com.nonononoki.alovoa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import java.util.Calendar;
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
import com.nonononoki.alovoa.entity.user.UserHide;
import com.nonononoki.alovoa.repo.CaptchaRepository;
import com.nonononoki.alovoa.repo.ContactRepository;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.UserHideRepository;
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
	}

	@Test
	public void testScheduleLong() throws Exception {
		// CLEAN-UP NON-CONFIRMED USERS
		var mockNewUsers = RegisterServiceTest.createMockNewUsers(captchaService, registerService, 3);
		User mockUser1 = mockNewUsers.get(0);
		User mockUser2 = mockNewUsers.get(1);
		User mockUser3 = mockNewUsers.get(2);

		try {
			// Non-confirmed user with more than 15 days creation date
			mockUser1.setConfirmed(false);
			var dates = mockUser1.getDates();
			dates.setCreationDate(addUnitsToDate(dates.getCreationDate(), Calendar.DATE, -15));

			// Non-confirmed user with 14 days creation date
			mockUser2.setConfirmed(false);
			dates = mockUser2.getDates();
			dates.setCreationDate(addUnitsToDate(dates.getCreationDate(), Calendar.DATE, -14));

			// Confirmed user with more than 15 days creation date
			mockUser3.setConfirmed(true);
			dates = mockUser3.getDates();
			dates.setCreationDate(addUnitsToDate(dates.getCreationDate(), Calendar.DATE, -15));
			userRepo.saveAllAndFlush(List.of(mockUser1, mockUser2, mockUser3));
			var initialCount = userRepo.count();

			scheduleService.cleanNonConfirmedUsers(new Date());
			assertEquals(2, (initialCount - userRepo.count()));
		} finally {
			RegisterServiceTest.deleteGivenUser(mockUser3, userService, userRepo, captchaService, authService);
		}
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

	private static Date addUnitsToDate(final Date inputDate, final int unitField, final int unitAmount) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(inputDate);
		calendar.add(unitField, unitAmount);

		return calendar.getTime();
	}
}
