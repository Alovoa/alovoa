package com.nonononoki.alovoa.html;

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
import com.nonononoki.alovoa.entity.user.UserBlock;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.service.AuthService;
import com.nonononoki.alovoa.service.CaptchaService;
import com.nonononoki.alovoa.service.MailService;
import com.nonononoki.alovoa.service.RegisterService;
import com.nonononoki.alovoa.service.RegisterServiceTest;
import com.nonononoki.alovoa.service.UserService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BlockedUsersResourceTest {

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

	@Value("${app.first-name.length-max}")
	private int firstNameLengthMax;

	@Value("${app.first-name.length-min}")
	private int firstNameLengthMin;

	@Autowired
	private BlockedUsersResource blockedUsersResource;

	@MockBean
	private AuthService authService;

	@MockBean
	private MailService mailService;

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

		User user = testUsers.get(0);
		User blockUser = testUsers.get(1);
		Mockito.when(authService.getCurrentUser()).thenReturn(user);
		Mockito.when(authService.getCurrentUser(true)).thenReturn(user);
		UserBlock block = new UserBlock();
		block.setUserFrom(user);
		block.setUserTo(blockUser);
		block.setDate(new Date());
		user.getBlockedUsers().add(block);
		userRepo.saveAndFlush(user);

		blockedUsersResource.blockedUsers();
	}
}
