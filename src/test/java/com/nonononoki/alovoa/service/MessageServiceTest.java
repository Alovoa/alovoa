package com.nonononoki.alovoa.service;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.Conversation;
import com.nonononoki.alovoa.entity.user.ConversationCheckedDate;
import com.nonononoki.alovoa.entity.user.Message;
import com.nonononoki.alovoa.entity.user.MessageReaction;
import com.nonononoki.alovoa.entity.user.UserBlock;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.MessageReactionDto;
import com.nonononoki.alovoa.model.ModerationResult;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.MessageReactionRepository;
import com.nonononoki.alovoa.repo.MessageRepository;
import com.nonononoki.alovoa.repo.UserBlockRepository;
import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.rest.ChatWebSocketController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MessageServiceTest {

    @Autowired
    private MessageService messageService;

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

    @Autowired
    private ApplicationContext applicationContext;

    @Value("${app.message.size}")
    private int maxMessageSize;

    @Value("${app.conversation.messages-max}")
    private int maxConvoMessages;

    @Value("${app.first-name.length-max}")
    private int firstNameLengthMax;

    @Value("${app.first-name.length-min}")
    private int firstNameLengthMin;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private MailService mailService;

    @MockitoBean
    private ContentModerationService moderationService;

    @MockitoBean
    private ChatWebSocketController chatWebSocketController;

    private List<User> testUsers;
    private Conversation testConversation;

    @BeforeEach
    void before() throws Exception {
        // Mock mail service
        Mockito.when(mailService.sendMail(any(String.class), any(String.class), any(String.class),
                any(String.class))).thenReturn(true);

        // Mock content moderation - allow all by default
        Mockito.when(moderationService.moderateContent(any(String.class), any(User.class), any(String.class)))
                .thenReturn(ModerationResult.allowed());

        // Create test users
        testUsers = RegisterServiceTest.getTestUsers(captchaService, registerService, firstNameLengthMax,
                firstNameLengthMin);

        // Create a test conversation between user1 and user2
        testConversation = createConversation(testUsers.get(0), testUsers.get(1));
    }

    @AfterEach
    void after() throws Exception {
        RegisterServiceTest.deleteAllUsers(userService, authService, captchaService, conversationRepo, userRepo);
    }

    // ============================================
    // Sending Messages Tests
    // ============================================

    @Test
    void testSendMessage_Success() throws Exception {
        User sender = testUsers.get(0);
        Mockito.doReturn(sender).when(authService).getCurrentUser(true);

        String messageContent = "Hello, this is a test message!";
        int initialMessageCount = testConversation.getMessages().size();

        messageService.send(testConversation.getId(), messageContent);

        // Refresh conversation
        testConversation = conversationRepo.findById(testConversation.getId()).orElse(null);
        assertNotNull(testConversation);
        assertEquals(initialMessageCount + 1, testConversation.getMessages().size());

        // Verify message content
        Message lastMessage = testConversation.getMessages().get(testConversation.getMessages().size() - 1);
        assertEquals(messageContent, lastMessage.getContent());
        assertEquals(sender, lastMessage.getUserFrom());
        assertEquals(testUsers.get(1), lastMessage.getUserTo());
        assertNotNull(lastMessage.getDate());
    }

    @Test
    void testSendMessage_WithURL_AllowedFormattingEnabled() throws Exception {
        User sender = testUsers.get(0);
        Mockito.doReturn(sender).when(authService).getCurrentUser(true);

        // Message must be a valid URL (entire content) for allowedFormatting to be enabled
        String validURL = "https://example.com";

        messageService.send(testConversation.getId(), validURL);

        testConversation = conversationRepo.findById(testConversation.getId()).orElse(null);
        Message lastMessage = testConversation.getMessages().get(testConversation.getMessages().size() - 1);

        assertTrue(lastMessage.isAllowedFormatting());
    }

    @Test
    void testSendMessage_ConversationNotFound() throws Exception {
        User sender = testUsers.get(0);
        Mockito.doReturn(sender).when(authService).getCurrentUser(true);

        assertThrows(AlovoaException.class, () -> {
            messageService.send(999999L, "Test message");
        });
    }

    @Test
    void testSendMessage_UserNotInConversation() throws Exception {
        User nonParticipant = testUsers.get(2); // User3 is not in the conversation
        Mockito.doReturn(nonParticipant).when(authService).getCurrentUser(true);

        assertThrows(AlovoaException.class, () -> {
            messageService.send(testConversation.getId(), "Test message");
        });
    }

    @Test
    void testSendMessage_MessageTooLong() throws Exception {
        User sender = testUsers.get(0);
        Mockito.doReturn(sender).when(authService).getCurrentUser(true);

        String tooLongMessage = "a".repeat(maxMessageSize + 1);

        assertThrows(AlovoaException.class, () -> {
            messageService.send(testConversation.getId(), tooLongMessage);
        });
    }

    @Test
    void testSendMessage_ContentBlocked() throws Exception {
        User sender = testUsers.get(0);
        Mockito.doReturn(sender).when(authService).getCurrentUser(true);

        // Mock moderation service to block content
        Mockito.when(moderationService.moderateContent(any(String.class), any(User.class), any(String.class)))
                .thenReturn(ModerationResult.blocked(0.9, Collections.singletonList("PROFANITY"), "Blocked"));

        assertThrows(AlovoaException.class, () -> {
            messageService.send(testConversation.getId(), "Bad content");
        });
    }

    @Test
    void testSendMessage_RecipientBlockedSender() throws Exception {
        User sender = testUsers.get(0);
        User recipient = testUsers.get(1);
        Mockito.doReturn(sender).when(authService).getCurrentUser(true);

        // Recipient blocks sender
        blockUser(recipient, sender);

        assertThrows(AlovoaException.class, () -> {
            messageService.send(testConversation.getId(), "Test message");
        });
    }

    @Test
    void testSendMessage_SenderBlockedRecipient() throws Exception {
        User sender = testUsers.get(0);
        User recipient = testUsers.get(1);
        Mockito.doReturn(sender).when(authService).getCurrentUser(true);

        // Sender blocks recipient
        blockUser(sender, recipient);

        assertThrows(AlovoaException.class, () -> {
            messageService.send(testConversation.getId(), "Test message");
        });
    }

    @Test
    void testSendMessage_EmailNotificationSent() throws Exception {
        User sender = testUsers.get(0);
        User recipient = testUsers.get(1);
        Mockito.doReturn(sender).when(authService).getCurrentUser(true);

        // Enable email notifications for recipient
        recipient.getUserSettings().setEmailChat(true);
        userRepo.saveAndFlush(recipient);

        String messageContent = "Test notification";
        messageService.send(testConversation.getId(), messageContent);

        // Verify mail service was called
        Mockito.verify(mailService, Mockito.times(1))
                .sendChatNotificationMail(eq(recipient), eq(sender), eq(messageContent), any(Conversation.class));
    }

    @Test
    void testSendMessage_MaxConversationMessagesLimit() throws Exception {
        User sender = testUsers.get(0);
        Mockito.doReturn(sender).when(authService).getCurrentUser(true);

        // Fill conversation with max messages + some extra
        for (int i = 0; i < maxConvoMessages + 5; i++) {
            messageService.send(testConversation.getId(), "Message " + i);
        }

        testConversation = conversationRepo.findById(testConversation.getId()).orElse(null);

        // Should not exceed maxConvoMessages
        assertTrue(testConversation.getMessages().size() <= maxConvoMessages);
    }

    @Test
    void testSendMessage_UpdatesConversationLastUpdated() throws Exception {
        User sender = testUsers.get(0);
        Mockito.doReturn(sender).when(authService).getCurrentUser(true);

        Date beforeSend = new Date();
        Thread.sleep(10); // Small delay to ensure timestamp difference

        messageService.send(testConversation.getId(), "Test message");

        testConversation = conversationRepo.findById(testConversation.getId()).orElse(null);
        assertNotNull(testConversation.getLastUpdated());
        assertTrue(testConversation.getLastUpdated().after(beforeSend) ||
                   testConversation.getLastUpdated().equals(beforeSend));
    }

    // ============================================
    // Message Read Receipts Tests
    // ============================================

    @Test
    void testMarkConversationAsRead_Success() throws Exception {
        User sender = testUsers.get(0);
        User recipient = testUsers.get(1);

        // Sender sends a message
        Mockito.doReturn(sender).when(authService).getCurrentUser(true);
        messageService.send(testConversation.getId(), "Test message");

        // Recipient marks conversation as read
        Mockito.doReturn(recipient).when(authService).getCurrentUser(true);
        messageService.markConversationAsRead(recipient, testConversation.getId());

        // Verify message is marked as read
        List<Message> messages = messageRepo.findUnreadMessagesInConversation(testConversation, recipient);
        assertEquals(0, messages.size());
    }

    @Test
    void testMarkConversationAsRead_ConversationNotFound() throws Exception {
        User user = testUsers.get(0);

        assertThrows(AlovoaException.class, () -> {
            messageService.markConversationAsRead(user, 999999L);
        });
    }

    @Test
    void testMarkConversationAsRead_UserNotInConversation() throws Exception {
        User nonParticipant = testUsers.get(2);

        assertThrows(AlovoaException.class, () -> {
            messageService.markConversationAsRead(nonParticipant, testConversation.getId());
        });
    }

    @Test
    void testMarkConversationAsRead_AlsoMarksAsDelivered() throws Exception {
        User sender = testUsers.get(0);
        User recipient = testUsers.get(1);

        // Sender sends a message
        Mockito.doReturn(sender).when(authService).getCurrentUser(true);
        messageService.send(testConversation.getId(), "Test message");

        testConversation = conversationRepo.findById(testConversation.getId()).orElse(null);
        Message message = testConversation.getMessages().get(testConversation.getMessages().size() - 1);
        assertNull(message.getDeliveredAt());
        assertNull(message.getReadAt());

        // Recipient marks as read
        messageService.markConversationAsRead(recipient, testConversation.getId());

        message = messageRepo.findById(message.getId()).orElse(null);
        assertNotNull(message.getDeliveredAt());
        assertNotNull(message.getReadAt());
    }

    @Test
    void testMarkConversationAsRead_NoUnreadMessages() throws Exception {
        User recipient = testUsers.get(1);

        // No exception should be thrown
        assertDoesNotThrow(() -> {
            messageService.markConversationAsRead(recipient, testConversation.getId());
        });
    }

    // ============================================
    // Message Delivery Receipts Tests
    // ============================================

    @Test
    void testMarkConversationAsDelivered_Success() throws Exception {
        User sender = testUsers.get(0);
        User recipient = testUsers.get(1);

        // Sender sends a message
        Mockito.doReturn(sender).when(authService).getCurrentUser(true);
        messageService.send(testConversation.getId(), "Test message");

        testConversation = conversationRepo.findById(testConversation.getId()).orElse(null);
        Message message = testConversation.getMessages().get(testConversation.getMessages().size() - 1);
        assertNull(message.getDeliveredAt());

        // Recipient marks as delivered
        messageService.markConversationAsDelivered(recipient, testConversation.getId());

        message = messageRepo.findById(message.getId()).orElse(null);
        assertNotNull(message.getDeliveredAt());
        assertNull(message.getReadAt()); // Should not be marked as read
    }

    @Test
    void testMarkConversationAsDelivered_ConversationNotFound() throws Exception {
        User user = testUsers.get(0);

        assertThrows(AlovoaException.class, () -> {
            messageService.markConversationAsDelivered(user, 999999L);
        });
    }

    @Test
    void testMarkConversationAsDelivered_UserNotInConversation() throws Exception {
        User nonParticipant = testUsers.get(2);

        assertThrows(AlovoaException.class, () -> {
            messageService.markConversationAsDelivered(nonParticipant, testConversation.getId());
        });
    }

    // ============================================
    // Message Reactions Tests
    // ============================================

    @Test
    void testAddReaction_Success() throws Exception {
        User sender = testUsers.get(0);
        User reactor = testUsers.get(1);

        // Sender sends a message
        Mockito.doReturn(sender).when(authService).getCurrentUser(true);
        messageService.send(testConversation.getId(), "Test message");

        testConversation = conversationRepo.findById(testConversation.getId()).orElse(null);
        Message message = testConversation.getMessages().get(testConversation.getMessages().size() - 1);
        Long messageId = message.getId();

        // Reactor adds a reaction
        String emoji = "❤️";
        MessageReactionDto reactionDto = messageService.addReaction(reactor, messageId, emoji);

        assertNotNull(reactionDto);
        assertEquals(messageId, reactionDto.getMessageId());
        assertEquals(emoji, reactionDto.getEmoji());
        assertEquals(reactor.getId(), reactionDto.getUserId());

        // Verify reaction was saved
        MessageReaction savedReaction = messageReactionRepo.findByMessageAndUser(message, reactor).orElse(null);
        assertNotNull(savedReaction);
        assertEquals(emoji, savedReaction.getEmoji());
    }

    @Test
    void testAddReaction_UpdateExistingReaction() throws Exception {
        User sender = testUsers.get(0);
        User reactor = testUsers.get(1);

        // Sender sends a message
        Mockito.doReturn(sender).when(authService).getCurrentUser(true);
        messageService.send(testConversation.getId(), "Test message");

        testConversation = conversationRepo.findById(testConversation.getId()).orElse(null);
        Message message = testConversation.getMessages().get(testConversation.getMessages().size() - 1);
        Long messageId = message.getId();

        // Add first reaction
        String firstEmoji = "❤️";
        messageService.addReaction(reactor, messageId, firstEmoji);

        // Update with new emoji
        String secondEmoji = "👍";
        MessageReactionDto reactionDto = messageService.addReaction(reactor, messageId, secondEmoji);

        assertEquals(secondEmoji, reactionDto.getEmoji());

        // Verify only one reaction exists
        long reactionCount = messageReactionRepo.countByMessage(message);
        assertEquals(1, reactionCount);
    }

    @Test
    void testAddReaction_MessageNotFound() throws Exception {
        User reactor = testUsers.get(1);

        assertThrows(AlovoaException.class, () -> {
            messageService.addReaction(reactor, 999999L, "❤️");
        });
    }

    @Test
    void testAddReaction_UserNotInConversation() throws Exception {
        User sender = testUsers.get(0);
        User nonParticipant = testUsers.get(2);

        // Sender sends a message
        Mockito.doReturn(sender).when(authService).getCurrentUser(true);
        messageService.send(testConversation.getId(), "Test message");

        testConversation = conversationRepo.findById(testConversation.getId()).orElse(null);
        Message message = testConversation.getMessages().get(testConversation.getMessages().size() - 1);

        // Non-participant tries to react
        assertThrows(AlovoaException.class, () -> {
            messageService.addReaction(nonParticipant, message.getId(), "❤️");
        });
    }

    @Test
    void testRemoveReaction_Success() throws Exception {
        User sender = testUsers.get(0);
        User reactor = testUsers.get(1);

        // Sender sends a message
        Mockito.doReturn(sender).when(authService).getCurrentUser(true);
        messageService.send(testConversation.getId(), "Test message");

        testConversation = conversationRepo.findById(testConversation.getId()).orElse(null);
        Message message = testConversation.getMessages().get(testConversation.getMessages().size() - 1);
        Long messageId = message.getId();

        // Add reaction
        messageService.addReaction(reactor, messageId, "❤️");

        // Verify reaction exists
        MessageReaction reaction = messageReactionRepo.findByMessageAndUser(message, reactor).orElse(null);
        assertNotNull(reaction);

        // Remove reaction
        messageService.removeReaction(reactor, messageId);

        // Verify reaction was removed
        reaction = messageReactionRepo.findByMessageAndUser(message, reactor).orElse(null);
        assertNull(reaction);
    }

    @Test
    void testRemoveReaction_NoReactionToRemove() throws Exception {
        User sender = testUsers.get(0);
        User reactor = testUsers.get(1);

        // Sender sends a message
        Mockito.doReturn(sender).when(authService).getCurrentUser(true);
        messageService.send(testConversation.getId(), "Test message");

        testConversation = conversationRepo.findById(testConversation.getId()).orElse(null);
        Message message = testConversation.getMessages().get(testConversation.getMessages().size() - 1);

        // No exception should be thrown
        assertDoesNotThrow(() -> {
            messageService.removeReaction(reactor, message.getId());
        });
    }

    @Test
    void testRemoveReaction_MessageNotFound() throws Exception {
        User reactor = testUsers.get(1);

        assertThrows(AlovoaException.class, () -> {
            messageService.removeReaction(reactor, 999999L);
        });
    }

    @Test
    void testRemoveReaction_UserNotInConversation() throws Exception {
        User sender = testUsers.get(0);
        User nonParticipant = testUsers.get(2);

        // Sender sends a message
        Mockito.doReturn(sender).when(authService).getCurrentUser(true);
        messageService.send(testConversation.getId(), "Test message");

        testConversation = conversationRepo.findById(testConversation.getId()).orElse(null);
        Message message = testConversation.getMessages().get(testConversation.getMessages().size() - 1);

        // Non-participant tries to remove reaction
        assertThrows(AlovoaException.class, () -> {
            messageService.removeReaction(nonParticipant, message.getId());
        });
    }

    // ============================================
    // Conversation Checked Date Tests
    // ============================================

    @Test
    void testUpdateCheckedDate_FirstTime() throws Exception {
        User user = testUsers.get(0);
        Mockito.doReturn(user).when(authService).getCurrentUser(true);

        testConversation = conversationRepo.findById(testConversation.getId()).orElse(null);
        Date lastChecked = messageService.updateCheckedDate(testConversation);

        assertNull(lastChecked); // First time checking

        // Verify checked date was created
        testConversation = conversationRepo.findById(testConversation.getId()).orElse(null);
        ConversationCheckedDate checkedDate = testConversation.getCheckedDates().stream()
                .filter(d -> d.getUserId().equals(user.getId()))
                .findFirst()
                .orElse(null);

        assertNotNull(checkedDate);
        assertNotNull(checkedDate.getLastCheckedDate());
    }

    @Test
    void testUpdateCheckedDate_SubsequentCheck() throws Exception {
        User user = testUsers.get(0);
        Mockito.doReturn(user).when(authService).getCurrentUser(true);

        testConversation = conversationRepo.findById(testConversation.getId()).orElse(null);

        // First check
        messageService.updateCheckedDate(testConversation);

        Thread.sleep(10); // Small delay

        // Second check
        testConversation = conversationRepo.findById(testConversation.getId()).orElse(null);
        Date lastChecked = messageService.updateCheckedDate(testConversation);

        assertNotNull(lastChecked); // Should return previous check time
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
