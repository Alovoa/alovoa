package com.nonononoki.alovoa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.nonononoki.alovoa.entity.Captcha;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserPasswordToken;
import com.nonononoki.alovoa.model.PasswordChangeDto;
import com.nonononoki.alovoa.model.PasswordResetDto;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.UserPasswordTokenRepository;
import com.nonononoki.alovoa.repo.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PasswordServiceTest {

	@Autowired
	private RegisterService registerService;

	@Autowired
	private CaptchaService captchaService;

	@Autowired
	private UserService userService;

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private ConversationRepository conversationRepo;

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
	private PasswordService passwordService;

	@Autowired
	private UserPasswordTokenRepository userPasswordTokenRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

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
		PasswordResetDto passwordResetDto = new PasswordResetDto();
		Captcha captcha = captchaService.generate();
		passwordResetDto.setCaptchaId(captcha.getId());
		passwordResetDto.setCaptchaText(captcha.getText());
		passwordResetDto.setEmail(user1.getEmail());

		assertEquals(0, userPasswordTokenRepository.count());

		UserPasswordToken userPasswordToken = passwordService.resetPassword(passwordResetDto);

		assertEquals(1, userPasswordTokenRepository.count());

		String newPassword = "newPassword";
		PasswordChangeDto passwordChangeDto = new PasswordChangeDto();
		passwordChangeDto.setEmail(user1.getEmail());
		passwordChangeDto.setPassword(newPassword);
		passwordChangeDto.setToken(userPasswordToken.getContent());

		passwordService.changePassword(passwordChangeDto);

		user1 = userRepo.findById(user1.getId()).get();

		if (!passwordEncoder.matches(newPassword, user1.getPassword())) {
			throw new BadCredentialsException("");
		}

		assertEquals(0, userPasswordTokenRepository.count());
	}

}
