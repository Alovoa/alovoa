package com.nonononoki.alovoa.service;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.repo.UserSettingsRepository;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;


@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserSettingsServiceTest {

    @Autowired
    private RegisterService registerService;

    @Autowired
    private CaptchaService captchaService;

    @Value("${app.first-name.length-max}")
    private int firstNameLengthMax;

    @Value("${app.first-name.length-min}")
    private int firstNameLengthMin;

    @MockBean
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserSettingsService userSettingsService;

    @Autowired
    private ConversationRepository conversationRepo;

    @Autowired
    private UserSettingsRepository userSettingsRepo;

    @MockBean
    private MailService mailService;

    @Autowired
    private UserRepository userRepo;

    private List<User> testUsers;

    @BeforeEach
    void before() throws Exception {
        Mockito.when(mailService.sendMail(Mockito.any(String.class), any(String.class), any(String.class), any(String.class))).thenReturn(true);
        testUsers = RegisterServiceTest.getTestUsers(captchaService, registerService, firstNameLengthMax, firstNameLengthMin);
    }

    @AfterEach
    void after() throws Exception {
        RegisterServiceTest.deleteAllUsers(userService, authService, captchaService, conversationRepo, userRepo);
    }

    @Test
    void testUpdateUserSettings() throws AlovoaException {
        User user1 = testUsers.get(0);
        Mockito.when(authService.getCurrentUser()).thenReturn(user1);
        Mockito.when(authService.getCurrentUser(true)).thenReturn(user1);

        assertEquals(3, userSettingsRepo.count());

        userSettingsService.updateEmailLike(true);
        userSettingsService.updateEmailChat(true);

        assertTrue(user1.getUserSettings().isEmailLike());
        assertTrue(user1.getUserSettings().isEmailChat());

        userSettingsService.updateEmailLike(false);
        userSettingsService.updateEmailChat(false);

        assertFalse(user1.getUserSettings().isEmailLike());
        assertFalse(user1.getUserSettings().isEmailChat());

    }
}
