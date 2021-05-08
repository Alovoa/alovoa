package com.nonononoki.alovoa;

import java.util.List;

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
import com.nonononoki.alovoa.entity.Contact;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserReport;
import com.nonononoki.alovoa.model.ContactDto;
import com.nonononoki.alovoa.model.MailDto;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.repo.ContactRepository;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.UserReportRepository;
import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.service.AdminService;
import com.nonononoki.alovoa.service.AuthService;
import com.nonononoki.alovoa.service.CaptchaService;
import com.nonononoki.alovoa.service.ImprintService;
import com.nonononoki.alovoa.service.RegisterService;
import com.nonononoki.alovoa.service.UserService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AdminTest {

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
	
	@Autowired
	private AdminService adminService;
	
	@Autowired
	private ImprintService imprintService;
	
	@Autowired
	private ContactRepository contactRepo;

	@Test
	public void test() throws Exception {

		List<User> testUsers = UserTest.getTestUsers(captchaService, registerService, firstNameLengthMax, firstNameLengthMin);
		
		List<User> allUsers = userRepo.findAll();
		User adminUser = allUsers.get(0);
		User user1 = testUsers.get(0);
		
		Mockito.when(authService.getCurrentUser()).thenReturn(adminUser);
		
		MailDto mailDto = new MailDto();
		String mailBody = "mailBody";
		String mailSubject = "mailSubject";
		mailDto.setBody(mailBody);
		mailDto.setSubject(mailSubject);
		
		adminService.sendMailAll(mailDto);
		mailDto.setEmail(user1.getEmail());
		adminService.sendMailSingle(mailDto);
			
		adminService.hideContact(contactTest().getId());
		adminService.deleteReport(reportTest(testUsers, adminUser).getId()); 
		
		adminService.banUser(UserDto.encodeId(user1.getId(), textEncryptor));
		User bannedUser = userRepo.findById(user1.getId()).get();
		Assert.assertEquals(bannedUser.isDisabled(), true);
		
		UserTest.deleteAllUsers(userService, authService, captchaService, conversationRepo, userRepo);
	}
	
	private Contact contactTest() throws Exception {
		Assert.assertEquals(contactRepo.count(), 0);
		ContactDto contact = new ContactDto();
		Captcha captcha = captchaService.generate();
		contact.setCaptchaId(captcha.getId());
		contact.setCaptchaText(captcha.getText());
		String email = "test" + Tools.MAIL_TEST_DOMAIN;
		contact.setEmail(email);
		Contact c = imprintService.contact(contact);
		Assert.assertEquals(contactRepo.count(), 1);
		
		return c;
	}
	
	private UserReport reportTest(List<User> testUsers, User adminUser) throws Exception {
		
		User user1 = testUsers.get(0);
		User user2 = testUsers.get(1);
		
		Mockito.when(authService.getCurrentUser()).thenReturn(user1);
		
		Assert.assertEquals(userReportRepo.count(), 0);
		Captcha captchaReport = captchaService.generate();
		UserReport report = userService.reportUser(UserDto.encodeId(user2.getId(), textEncryptor), captchaReport.getId(), captchaReport.getText(), "report");
		Assert.assertEquals(userReportRepo.count(), 1);
			
		Mockito.when(authService.getCurrentUser()).thenReturn(adminUser);
		
		return report;
	}
	
 }
