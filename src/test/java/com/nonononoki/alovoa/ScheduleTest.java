package com.nonononoki.alovoa;

import java.util.Date;
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
import com.nonononoki.alovoa.service.AuthService;
import com.nonononoki.alovoa.service.CaptchaService;
import com.nonononoki.alovoa.service.PasswordService;
import com.nonononoki.alovoa.service.RegisterService;
import com.nonononoki.alovoa.service.ScheduleService;
import com.nonononoki.alovoa.service.UserService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ScheduleTest {

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

	//@MockBean
	@Autowired
	private CaptchaService captchaService;

	@MockBean
	private AuthService authService;

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
	
	@Test
	public void test() throws Exception {
		
		//always valid on tests
		//Mockito.when(
		//		captchaService.isValid(Mockito.anyLong(), Mockito.nullable(String.class)))			
		//.thenReturn(true);
		
		//UserTest userTest = new UserTest();
		//List<User> testUsers = userTest.getTestUsers();
		Assert.assertEquals(userRepo.count(), 1);
		List<User> testUsers = UserTest.getTestUsers(captchaService, registerService, firstNameLengthMax, firstNameLengthMin);
		Assert.assertEquals(userRepo.count(), 4);
		
		Date currentDate = new Date();
		long currentDateTime = currentDate.getTime();
		
		//CAPTCHA 
		Captcha captchaOld = generateCaptcha();
		captchaOld.setDate(new Date(currentDateTime - captchaDelay - 1 ));
		catchaRepo.saveAndFlush(captchaOld);

		Captcha captchaNew = generateCaptcha();
		captchaNew.setDate(new Date(currentDateTime - captchaDelay));
		catchaRepo.saveAndFlush(captchaNew);

		Assert.assertEquals(catchaRepo.count(), 2);
		scheduleService.cleanCaptcha(currentDate);
		Assert.assertEquals(catchaRepo.count(), 1);
		
		//CONTACT 
		Contact contactOld = new Contact();
		contactOld.setEmail("test" + Tools.MAIL_TEST_DOMAIN);
		contactOld.setHidden(false);
		contactOld.setMessage("test message");
		contactOld.setDate(new Date(currentDateTime - contactDelay - 1 ));
		contactRepo.saveAndFlush(contactOld);

		Contact contactNew = new Contact();
		contactNew.setEmail("test" + Tools.MAIL_TEST_DOMAIN);
		contactNew.setHidden(false);
		contactNew.setMessage("test message");
		contactNew.setDate(new Date(currentDateTime - contactDelay));
		contactRepo.saveAndFlush(contactNew);

		Assert.assertEquals(contactRepo.count(), 2);
		scheduleService.cleanContact(currentDate);
		Assert.assertEquals(contactRepo.count(), 1);

		
		//USERHIDE
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
		
		Assert.assertEquals(userHideRepo.count(), 2);
		scheduleService.cleanUserHide(currentDate);
		Assert.assertEquals(userHideRepo.count(), 1);
		
		//USERPASSWORDTOKEN
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
		
		Assert.assertEquals(passwordTokenRepository.count(), 2);
		scheduleService.cleanUserPasswordToken(currentDate);
		Assert.assertEquals(passwordTokenRepository.count(), 1);

		//USERDELETETOKEN	
		Mockito.when(authService.getCurrentUser()).thenReturn(user1);
		UserDeleteToken deleteTokenOld = userService.deleteAccountRequest();
		Mockito.when(authService.getCurrentUser()).thenReturn(user2);
		UserDeleteToken deleteTokenNew = userService.deleteAccountRequest();	
		
		Assert.assertEquals(userDeleteTokenRepository.count(), 2);
		deleteTokenOld.setDate(new Date(currentDateTime - passwordResetDelay -1));
		user1.setDeleteToken(deleteTokenNew);
		userRepo.saveAndFlush(user1);
		
		deleteTokenNew.setDate(new Date(currentDateTime - passwordResetDelay));
		user2.setDeleteToken(deleteTokenNew);
		userRepo.saveAndFlush(user2);
		
		scheduleService.cleanUserDeleteToken(currentDate);
		Assert.assertEquals(userDeleteTokenRepository.count(), 1);
		
		UserTest.deleteAllUsers(userService, authService, captchaService, conversationRepo, userRepo);
	}
	
	private Captcha generateCaptcha() {
		Captcha captcha = new Captcha();
		captcha.setDate(new Date());
		captcha.setImage(null);
		captcha.setText("test");
		captcha.setIp(null);
		captcha = captchaRepo.saveAndFlush(captcha);
		return captcha;
	}
}
