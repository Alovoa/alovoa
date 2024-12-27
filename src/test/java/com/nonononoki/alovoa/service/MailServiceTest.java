package com.nonononoki.alovoa.service;

import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.*;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MailServiceTest {

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

    @Autowired
    private TextEncryptorConverter textEncryptor;

    @MockitoBean
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private MailService mailService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private ConversationRepository conversationRepo;

    private List<User> testUsers;

    @BeforeEach
    void before() throws Exception {
        testUsers = RegisterServiceTest.getTestUsers(captchaService, registerService, firstNameLengthMax, firstNameLengthMin);
    }

    @AfterEach
    void after() throws Exception {
        RegisterServiceTest.deleteAllUsers(userService, authService, captchaService, conversationRepo, userRepo);
    }

    @Disabled
    @Test
    void test() throws Exception {
        User user1 = testUsers.get(0);
        String subject = "test";
        String body = "test body";
        UserDeleteToken deleteToken = new UserDeleteToken();
        deleteToken.setContent("testToken");
        UserPasswordToken passwordToken = new UserPasswordToken();
        passwordToken.setContent("testToken");
        UserRegisterToken registerToken = new UserRegisterToken();
        registerToken.setContent("testToken");
        user1.setDeleteToken(deleteToken);
        user1.setPasswordToken(passwordToken);
        user1.setRegisterToken(registerToken);
        mailService.sendAdminMail(user1.getEmail(), subject, body);
        mailService.sendAccountConfirmed(user1);
        mailService.sendAccountDeleteConfirm(user1);
        mailService.sendAccountDeleteRequest(user1);
        mailService.sendPasswordResetMail(user1);
        mailService.sendRegistrationMail(user1);
    }


    @Test
    void testChatMailSend() throws AlovoaException {
        User user1 = testUsers.get(0);
        User user2 = testUsers.get(1);

        when(authService.getCurrentUser()).thenReturn(user1);
        when(authService.getCurrentUser(true)).thenReturn(user1);

        Message message1 = new Message();
        message1.setUserFrom(user1);
        Message message2 = new Message();
        message2.setUserFrom(user1);
        Conversation convo1 = mock(Conversation.class);
        when(convo1.getMessages()).thenReturn(List.of(message1));
        Conversation convo2 = mock(Conversation.class);
        when(convo2.getMessages()).thenReturn(List.of(message1, message2));

        assertTrue(mailService.sendChatNotificationMail(user2, user1, "message", convo1));
        assertFalse(mailService.sendChatNotificationMail(user2, user1, "message", convo2));
    }

}
