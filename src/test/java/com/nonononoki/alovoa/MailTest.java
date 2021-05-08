package com.nonononoki.alovoa;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.service.AuthService;
import com.nonononoki.alovoa.service.CaptchaService;
import com.nonononoki.alovoa.service.MailService;
import com.nonononoki.alovoa.service.RegisterService;
import com.nonononoki.alovoa.service.UserService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class MailTest {

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

	@Test
	public void test() throws Exception {
		
		List<User> testUsers = UserTest.getTestUsers(captchaService, registerService, firstNameLengthMax, firstNameLengthMin);
		User user1 = testUsers.get(1);
		String subject = "test";
		String body = "test body";
		mailService.sendAdminMail(user1.getEmail(), subject, body);
		UserTest.deleteAllUsers(userService, authService, captchaService, conversationRepo, userRepo);
		
	}
	
 }
