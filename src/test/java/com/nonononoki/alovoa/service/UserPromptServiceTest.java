package com.nonononoki.alovoa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.UserPromptDto;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.UserPromptRepository;
import com.nonononoki.alovoa.repo.UserRepository;
import org.apache.commons.lang3.StringUtils;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserPromptServiceTest {

    @Autowired
    private RegisterService registerService;
    @Autowired
    private CaptchaService captchaService;
    @Autowired
    private UserService userService;
    @Autowired
    private UserPromptService userPromptService;
    @Autowired
    private UserPromptRepository userPromptRepo;
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private ConversationRepository conversationRepo;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TextEncryptorConverter textEncryptor;
    @MockBean
    private AuthService authService;
    @MockBean
    private MailService mailService;
    @Value("${app.first-name.length-max}")
    private int firstNameLengthMax;
    @Value("${app.first-name.length-min}")
    private int firstNameLengthMin;
    @Value("${app.prompt.max}")
    private int promptMax;
    @Value("${app.prompt.length.max}")
    private int promptLengthMax;
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
        User user1 = testUsers.get(0);
        Mockito.when(authService.getCurrentUser()).thenReturn(user1);
        Mockito.when(authService.getCurrentUser(true)).thenReturn(user1);
        Long promptId = 1L;
        long invalidPromptId = 999L;
        UserPromptDto prompt1 = UserPromptDto.builder().promptId(promptId).text("test").build();
        assertEquals(0, userPromptRepo.count());
        userPromptService.addPrompt(prompt1);
        assertEquals(1, userPromptRepo.count());
        //add another prompt with same id
        assertThrows(AlovoaException.class, () -> {
            userPromptService.addPrompt(prompt1);
        });
        assertEquals(1, userPromptRepo.count());
        UserPromptDto prompt2 = UserPromptDto.builder().promptId(promptId).text(StringUtils.repeat('a', promptLengthMax)).build();
        userPromptService.updatePrompt(prompt2);
        UserPromptDto prompt3 = UserPromptDto.builder().promptId(invalidPromptId).text(StringUtils.repeat('a', promptLengthMax+1)).build();
        assertThrows(AlovoaException.class, () -> {
            userPromptService.updatePrompt(prompt3);
        });
        assertEquals(1, userPromptRepo.count());
        userPromptService.deletePrompt(promptId);
        assertEquals(0, userPromptRepo.count());
    }
}
