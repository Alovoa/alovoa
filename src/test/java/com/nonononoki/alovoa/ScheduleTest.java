package com.nonononoki.alovoa;

import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import com.nonononoki.alovoa.entity.Captcha;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserDeleteToken;
import com.nonononoki.alovoa.entity.user.UserHide;
import com.nonononoki.alovoa.entity.user.UserPasswordToken;
import com.nonononoki.alovoa.model.PasswordResetDto;
import com.nonononoki.alovoa.repo.CaptchaRepository;
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

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration
@ActiveProfiles("test")
@Transactional
public class ScheduleTest {

	@Autowired
	private CaptchaRepository captchaRepo;

	@Autowired
	private UserHideRepository userHideRepo;

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private UserPasswordTokenRepository passwordTokenRepository;

	@Autowired
	private UserDeleteTokenRepository userDeleteTokenRepository;
	
	@Autowired
	private ScheduleService scheduleService;

	@MockBean
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
		Mockito.when(
				captchaService.isValid(Mockito.anyLong(), Mockito.nullable(String.class)))			
		.thenReturn(true);
		
		//UserTest userTest = new UserTest();
		//List<User> testUsers = userTest.getTestUsers();
		Assert.assertEquals(userRepo.count(), 1);
		List<User> testUsers = UserTest.getTestUsers(captchaService, registerService);
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
		PasswordResetDto pwResetDto = new PasswordResetDto();
		pwResetDto.setEmail(user1.getEmail());
		UserPasswordToken passwordTokenOld = passwordService.resetPasword(pwResetDto);
		passwordTokenOld.setDate(new Date(currentDateTime - passwordResetDelay - 1));
		user1.setPasswordToken(passwordTokenOld);
		userRepo.saveAndFlush(user1);
		
		pwResetDto.setEmail(user2.getEmail());
		UserPasswordToken passwordTokenNew = passwordService.resetPasword(pwResetDto);
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
		
		UserTest.resetTestUsers();
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
