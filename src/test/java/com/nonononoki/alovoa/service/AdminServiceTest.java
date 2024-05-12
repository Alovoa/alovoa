package com.nonononoki.alovoa.service;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.Captcha;
import com.nonononoki.alovoa.entity.Contact;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserReport;
import com.nonononoki.alovoa.model.ContactDto;
import com.nonononoki.alovoa.model.MailDto;
import com.nonononoki.alovoa.repo.ContactRepository;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.UserReportRepository;
import com.nonononoki.alovoa.repo.UserRepository;
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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminServiceTest {

	@Autowired
	private RegisterService registerService;

	@Autowired
	private CaptchaService captchaService;

	@Autowired
	private UserService userService;

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private UserReportRepository userReportRepo;

	@Autowired
	private ConversationRepository conversationRepo;

	@Autowired
	private TextEncryptorConverter textEncryptor;

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
	private AdminService adminService;

	@Autowired
	private ImprintService imprintService;

	@Autowired
	private ContactRepository contactRepo;

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

		List<User> allUsers = userRepo.findAll();
		User adminUser = allUsers.get(0);
		User user1 = testUsers.get(0);
		User user2 = testUsers.get(1);
		User user3 = testUsers.get(2);

		Mockito.when(authService.getCurrentUser()).thenReturn(adminUser);
		Mockito.when(authService.getCurrentUser(true)).thenReturn(adminUser);

		MailDto mailDto = new MailDto();
		String mailBodyAll = "mailBodyAll";
		String mailBodySingle = "mailBodySingle";
		String mailSubject = "mailSubject";
		mailDto.setBody(mailBodyAll);
		mailDto.setSubject(mailSubject);

		adminService.sendMailAll(mailDto);
		mailDto.setEmail(user1.getEmail());
		mailDto.setBody(mailBodySingle);
		adminService.sendMailSingle(mailDto);

		adminService.hideContact(contactTest().getId());

		assertEquals(0, userReportRepo.count());
		reportTest(user3, user2, adminUser);
		assertEquals(1, userReportRepo.count());
		UserReport report = reportTest(user1, user2, adminUser);
		user2 = userRepo.findByEmail(user2.getEmail());
		adminService.deleteReport(report.getId());
		assertEquals(0, userReportRepo.count());

		adminService.banUser(user1.getUuid());
		User bannedUser = userRepo.findById(user1.getId()).get();
		assertTrue(bannedUser.isDisabled());
		assertTrue(adminService.userExists(URLDecoder.decode(user2.getEmail(), StandardCharsets.UTF_8)));

		double donationAmount = 10.00;
		adminService.addDonation(URLDecoder.decode(user2.getEmail(), StandardCharsets.UTF_8), donationAmount);
		assertEquals(donationAmount, user2.getTotalDonations());
		assertEquals(1, user2.getDonations().size());

	}

	private Contact contactTest() throws Exception {
		assertEquals(0, contactRepo.count());
		ContactDto contact = new ContactDto();
		Captcha captcha = captchaService.generate();
		contact.setCaptchaId(captcha.getId());
		contact.setCaptchaText(captcha.getText());
		String email = "test" + Tools.MAIL_TEST_DOMAIN;
		contact.setEmail(email);
		Contact c = imprintService.contact(contact);
		assertEquals(1, contactRepo.count());

		return c;
	}

	private UserReport reportTest(User user1, User user2, User adminUser) throws Exception {
		Mockito.when(authService.getCurrentUser()).thenReturn(user1);
		Mockito.when(authService.getCurrentUser(true)).thenReturn(user1);
		UserReport report = userService.reportUser(user2.getUuid(), "report");
		Mockito.when(authService.getCurrentUser()).thenReturn(adminUser);
		Mockito.when(authService.getCurrentUser(true)).thenReturn(adminUser);
		return report;
	}

}
