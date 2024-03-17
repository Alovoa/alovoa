package com.nonononoki.alovoa.service;

import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserDeleteToken;
import com.nonononoki.alovoa.entity.user.UserPasswordToken;
import com.nonononoki.alovoa.entity.user.UserRegisterToken;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.repo.ConversationRepository;
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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

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

    @MockBean
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
    void testLikeMatchAndChatEmails() throws AlovoaException, GeneralSecurityException, IOException {
        User user1 = testUsers.get(0);
        User user2 = testUsers.get(1);

        //user1 likes user2
        Mockito.when(authService.getCurrentUser()).thenReturn(user1);
        Mockito.when(authService.getCurrentUser(true)).thenReturn(user1);

        String idEnc2 = UserDto.encodeId(user2.getId(), textEncryptor);
        userService.likeUser(idEnc2, null);

        //user2 likes user1
        Mockito.when(authService.getCurrentUser()).thenReturn(user2);
        Mockito.when(authService.getCurrentUser(true)).thenReturn(user2);

        String idEnc1 = UserDto.encodeId(user1.getId(), textEncryptor);
        userService.likeUser(idEnc1, null);

        //match must occur since both like each other.

        //user2 sends a message to user1
        messageService.send(user1.getConversations().get(0).getId(), "Hello this is a test message.");
    }

}
