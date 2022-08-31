package com.nonononoki.alovoa.html;

import static org.mockito.ArgumentMatchers.any;

import java.util.ArrayList;
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
import com.nonononoki.alovoa.entity.user.Conversation;
import com.nonononoki.alovoa.entity.user.Message;
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
class MessageResourceTest {

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
	private MessageResource messageResource;

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

		User user = testUsers.get(2);
		User currUser = testUsers.get(1);

		Mockito.when(authService.getCurrentUser()).thenReturn(currUser);
		Mockito.when(authService.getCurrentUser(true)).thenReturn(currUser);
		messageResource.chats();

		List<Message> messages = new ArrayList<>();

		Message message = new Message();
		message.setAllowedFormatting(false);
		message.setContent("test");
		message.setDate(new Date());
		message.setUserFrom(currUser);
		message.setUserTo(user);

		messages.add(message);

		Conversation convo = new Conversation();
		convo.setUsers(new ArrayList<>());
		convo.setDate(new Date());
		convo.getUsers().add(currUser);
		convo.getUsers().add(user);
		convo.setLastUpdated(new Date());
		convo.setMessages(messages);
		convo = conversationRepo.saveAndFlush(convo);

		messageResource.chatsDetail(convo.getId());
	}
}
