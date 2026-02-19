package com.nonononoki.alovoa.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.Conversation;
import com.nonononoki.alovoa.entity.user.ConversationCheckedDate;
import com.nonononoki.alovoa.entity.user.Message;
import com.nonononoki.alovoa.entity.user.UserBlock;
import com.nonononoki.alovoa.model.ModerationResult;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.MessageReactionRepository;
import com.nonononoki.alovoa.repo.MessageRepository;
import com.nonononoki.alovoa.repo.UserBlockRepository;
import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.rest.ChatWebSocketController;
import com.nonononoki.alovoa.service.*;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    @Autowired
    private MessageRepository messageRepo;

    @Autowired
    private MessageReactionRepository messageReactionRepo;

    @Autowired
    private UserBlockRepository userBlockRepo;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private MailService mailService;

    @MockitoBean
    private ContentModerationService moderationService;

    @MockitoBean
    private ChatWebSocketController chatWebSocketController;

    @Value("${app.first-name.length-max}")
    private int firstNameLengthMax;

    @Value("${app.first-name.length-min}")
    private int firstNameLengthMin;

    private List<User> testUsers;
    private Conversation testConversation;

    @BeforeEach
    void before() throws Exception {
        // Mock mail service
        Mockito.when(mailService.sendMail(any(String.class), any(String.class), any(String.class), any(String.class)))
                .thenReturn(true);

        // Mock content moderation - allow all by default
        Mockito.when(moderationService.moderateContent(any(String.class), any(User.class), any(String.class)))
                .thenReturn(ModerationResult.allowed());

        testUsers = RegisterServiceTest.getTestUsers(captchaService, registerService, firstNameLengthMax, firstNameLengthMin);

        // Create a test conversation between user1 and user2
        testConversation = createConversation(testUsers.get(0), testUsers.get(1));
    }

    @AfterEach
    void after() throws Exception {
        RegisterServiceTest.deleteAllUsers(userService, authService, captchaService, conversationRepo, userRepo);
    }

    @Test
    @WithMockUser
    @DisplayName("POST /message/send/{convoId} - Send message successfully")
    void testSendMessage() throws Exception {
        User sender = testUsers.get(0);
        Mockito.doReturn(sender).when(authService).getCurrentUser(true);

        String messageContent = "Hello, this is a test message!";
        int initialMessageCount = testConversation.getMessages().size();

        mockMvc.perform(post("/message/send/" + testConversation.getId())
                        .contentType("text/plain")
                        .content(messageContent))
                .andExpect(status().isOk());

        // Verify message was added
        testConversation = conversationRepo.findById(testConversation.getId()).orElse(null);
        Assertions.assertNotNull(testConversation);
        Assertions.assertEquals(initialMessageCount + 1, testConversation.getMessages().size());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /message/send/{convoId} - Conversation not found returns error")
    void testSendMessageConversationNotFound() throws Exception {
        User sender = testUsers.get(0);
        Mockito.doReturn(sender).when(authService).getCurrentUser(true);

        // AlovoaException is caught by global ExceptionHandler which returns 409 Conflict
        mockMvc.perform(post("/message/send/999999")
                        .contentType("text/plain")
                        .content("Test message"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /message/send/{convoId} - User not in conversation returns error")
    void testSendMessageUserNotInConversation() throws Exception {
        User nonParticipant = testUsers.get(2);
        Mockito.doReturn(nonParticipant).when(authService).getCurrentUser(true);

        // AlovoaException is caught by global ExceptionHandler which returns 409 Conflict
        mockMvc.perform(post("/message/send/" + testConversation.getId())
                        .contentType("text/plain")
                        .content("Test message"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /message/read/{conversationId} - Mark conversation as read")
    void testMarkAsRead() throws Exception {
        User sender = testUsers.get(0);
        User recipient = testUsers.get(1);

        // Sender sends a message
        Mockito.doReturn(sender).when(authService).getCurrentUser(true);
        mockMvc.perform(post("/message/send/" + testConversation.getId())
                        .contentType("text/plain")
                        .content("Test message"))
                .andExpect(status().isOk());

        // Recipient marks as read
        Mockito.doReturn(recipient).when(authService).getCurrentUser(true);
        mockMvc.perform(post("/message/read/" + testConversation.getId()))
                .andExpect(status().isOk());

        // Verify no unread messages for recipient
        List<Message> unreadMessages = messageRepo.findUnreadMessagesInConversation(testConversation, recipient);
        Assertions.assertEquals(0, unreadMessages.size());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /message/read/{conversationId} - Conversation not found returns error")
    void testMarkAsReadConversationNotFound() throws Exception {
        User user = testUsers.get(0);
        Mockito.doReturn(user).when(authService).getCurrentUser(true);

        // AlovoaException is caught by global ExceptionHandler which returns 409 Conflict
        mockMvc.perform(post("/message/read/999999"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /message/delivered/{conversationId} - Mark conversation as delivered")
    void testMarkAsDelivered() throws Exception {
        User sender = testUsers.get(0);
        User recipient = testUsers.get(1);

        // Sender sends a message
        Mockito.doReturn(sender).when(authService).getCurrentUser(true);
        mockMvc.perform(post("/message/send/" + testConversation.getId())
                        .contentType("text/plain")
                        .content("Test message"))
                .andExpect(status().isOk());

        testConversation = conversationRepo.findById(testConversation.getId()).orElse(null);
        Message message = testConversation.getMessages().get(testConversation.getMessages().size() - 1);
        Assertions.assertNull(message.getDeliveredAt());

        // Recipient marks as delivered
        Mockito.doReturn(recipient).when(authService).getCurrentUser(true);
        mockMvc.perform(post("/message/delivered/" + testConversation.getId()))
                .andExpect(status().isOk());

        // Verify message is marked as delivered
        message = messageRepo.findById(message.getId()).orElse(null);
        Assertions.assertNotNull(message.getDeliveredAt());
        Assertions.assertNull(message.getReadAt()); // Should not be marked as read
    }

    @Test
    @WithMockUser
    @DisplayName("POST /message/{messageId}/react - Add reaction to message")
    void testAddReaction() throws Exception {
        User sender = testUsers.get(0);
        User reactor = testUsers.get(1);

        // Sender sends a message
        Mockito.doReturn(sender).when(authService).getCurrentUser(true);
        mockMvc.perform(post("/message/send/" + testConversation.getId())
                        .contentType("text/plain")
                        .content("Test message"))
                .andExpect(status().isOk());

        testConversation = conversationRepo.findById(testConversation.getId()).orElse(null);
        Message message = testConversation.getMessages().get(testConversation.getMessages().size() - 1);
        Long messageId = message.getId();

        // Reactor adds a reaction
        Mockito.doReturn(reactor).when(authService).getCurrentUser(true);
        String emoji = "❤️";

        mockMvc.perform(post("/message/" + messageId + "/react")
                        .contentType("text/plain")
                        .content(emoji))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value(messageId))
                .andExpect(jsonPath("$.emoji").value(emoji))
                .andExpect(jsonPath("$.userId").value(reactor.getId()));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /message/{messageId}/react - Message not found returns error")
    void testAddReactionMessageNotFound() throws Exception {
        User reactor = testUsers.get(1);
        Mockito.doReturn(reactor).when(authService).getCurrentUser(true);

        // AlovoaException is caught by global ExceptionHandler which returns 409 Conflict
        mockMvc.perform(post("/message/999999/react")
                        .contentType("text/plain")
                        .content("❤️"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /message/{messageId}/react - User not in conversation returns error")
    void testAddReactionUserNotInConversation() throws Exception {
        User sender = testUsers.get(0);
        User nonParticipant = testUsers.get(2);

        // Sender sends a message
        Mockito.doReturn(sender).when(authService).getCurrentUser(true);
        mockMvc.perform(post("/message/send/" + testConversation.getId())
                        .contentType("text/plain")
                        .content("Test message"))
                .andExpect(status().isOk());

        testConversation = conversationRepo.findById(testConversation.getId()).orElse(null);
        Message message = testConversation.getMessages().get(testConversation.getMessages().size() - 1);

        // Non-participant tries to react
        Mockito.doReturn(nonParticipant).when(authService).getCurrentUser(true);
        // AlovoaException is caught by global ExceptionHandler which returns 409 Conflict
        mockMvc.perform(post("/message/" + message.getId() + "/react")
                        .contentType("text/plain")
                        .content("❤️"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /message/{messageId}/react - Remove reaction from message")
    void testRemoveReaction() throws Exception {
        User sender = testUsers.get(0);
        User reactor = testUsers.get(1);

        // Sender sends a message
        Mockito.doReturn(sender).when(authService).getCurrentUser(true);
        mockMvc.perform(post("/message/send/" + testConversation.getId())
                        .contentType("text/plain")
                        .content("Test message"))
                .andExpect(status().isOk());

        testConversation = conversationRepo.findById(testConversation.getId()).orElse(null);
        Message message = testConversation.getMessages().get(testConversation.getMessages().size() - 1);
        Long messageId = message.getId();

        // Reactor adds a reaction
        Mockito.doReturn(reactor).when(authService).getCurrentUser(true);
        mockMvc.perform(post("/message/" + messageId + "/react")
                        .contentType("text/plain")
                        .content("❤️"))
                .andExpect(status().isOk());

        // Remove the reaction
        mockMvc.perform(delete("/message/" + messageId + "/react"))
                .andExpect(status().isOk());

        // Verify reaction was removed
        Assertions.assertFalse(messageReactionRepo.findByMessageAndUser(message, reactor).isPresent());
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /message/{messageId}/react - Message not found returns error")
    void testRemoveReactionMessageNotFound() throws Exception {
        User reactor = testUsers.get(1);
        Mockito.doReturn(reactor).when(authService).getCurrentUser(true);

        // AlovoaException is caught by global ExceptionHandler which returns 409 Conflict
        mockMvc.perform(delete("/message/999999/react"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /message/get-messages/{convoId}/{first} - Get messages for conversation")
    void testGetMessages() throws Exception {
        User sender = testUsers.get(0);
        User recipient = testUsers.get(1);

        // Sender sends several messages
        Mockito.doReturn(sender).when(authService).getCurrentUser(true);
        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(post("/message/send/" + testConversation.getId())
                            .contentType("text/plain")
                            .content("Message " + i))
                    .andExpect(status().isOk());
        }

        // Recipient gets messages
        Mockito.doReturn(recipient).when(authService).getCurrentUser(true);
        mockMvc.perform(get("/message/get-messages/" + testConversation.getId() + "/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /message/get-messages/{convoId}/{first} - Conversation not found returns error")
    void testGetMessagesConversationNotFound() throws Exception {
        User user = testUsers.get(0);
        Mockito.doReturn(user).when(authService).getCurrentUser(true);

        // AlovoaException is caught by global ExceptionHandler which returns 409 Conflict
        mockMvc.perform(get("/message/get-messages/999999/1"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /message/get-messages/{convoId}/{first} - User not in conversation returns error")
    void testGetMessagesUserNotInConversation() throws Exception {
        User nonParticipant = testUsers.get(2);
        Mockito.doReturn(nonParticipant).when(authService).getCurrentUser(true);

        // AlovoaException is caught by global ExceptionHandler which returns 409 Conflict
        mockMvc.perform(get("/message/get-messages/" + testConversation.getId() + "/1"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /message/get-messages/{convoId}/{first} - Blocked user returns error")
    void testGetMessagesBlockedUser() throws Exception {
        User user1 = testUsers.get(0);
        User user2 = testUsers.get(1);

        // User2 blocks User1
        blockUser(user2, user1);

        // User1 tries to get messages
        Mockito.doReturn(user1).when(authService).getCurrentUser(true);
        // AlovoaException is caught by global ExceptionHandler which returns 409 Conflict
        mockMvc.perform(get("/message/get-messages/" + testConversation.getId() + "/1"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /message/send/{convoId} - Blocked by partner returns error")
    void testSendMessageBlockedByPartner() throws Exception {
        User sender = testUsers.get(0);
        User recipient = testUsers.get(1);

        // Recipient blocks sender
        blockUser(recipient, sender);

        // Sender tries to send message
        Mockito.doReturn(sender).when(authService).getCurrentUser(true);
        // AlovoaException is caught by global ExceptionHandler which returns 409 Conflict
        mockMvc.perform(post("/message/send/" + testConversation.getId())
                        .contentType("text/plain")
                        .content("Test message"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /message/send/{convoId} - Content moderation blocks message")
    void testSendMessageContentBlocked() throws Exception {
        User sender = testUsers.get(0);
        Mockito.doReturn(sender).when(authService).getCurrentUser(true);

        // Mock moderation service to block content
        Mockito.when(moderationService.moderateContent(any(String.class), any(User.class), any(String.class)))
                .thenReturn(ModerationResult.blocked(0.9, Collections.singletonList("PROFANITY"), "Blocked"));

        // AlovoaException is caught by global ExceptionHandler which returns 409 Conflict
        mockMvc.perform(post("/message/send/" + testConversation.getId())
                        .contentType("text/plain")
                        .content("Bad content"))
                .andExpect(status().isConflict());
    }

    // ============================================
    // Helper Methods
    // ============================================

    /**
     * Creates a conversation between two users
     */
    private Conversation createConversation(User user1, User user2) {
        Conversation conversation = new Conversation();
        conversation.setUsers(new ArrayList<>());
        conversation.setDate(new Date());
        conversation.addUser(user1);
        conversation.addUser(user2);
        conversation.setLastUpdated(new Date());
        conversation.setMessages(new ArrayList<>());
        conversation.setCheckedDates(new ArrayList<>());
        conversation = conversationRepo.saveAndFlush(conversation);

        user1.addConversation(conversation);
        user2.addConversation(conversation);
        userRepo.saveAndFlush(user1);
        userRepo.saveAndFlush(user2);

        return conversation;
    }

    /**
     * Creates a block relationship between two users
     */
    private void blockUser(User blocker, User blocked) {
        UserBlock block = new UserBlock();
        block.setUserFrom(blocker);
        block.setUserTo(blocked);
        block.setDate(new Date());
        userBlockRepo.saveAndFlush(block);

        blocker.getBlockedUsers().add(block);
        userRepo.saveAndFlush(blocker);
    }
}
