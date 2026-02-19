package com.nonononoki.alovoa.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.Conversation;
import com.nonononoki.alovoa.entity.user.ConversationCheckedDate;
import com.nonononoki.alovoa.entity.user.Message;
import com.nonononoki.alovoa.entity.user.MessageReaction;
import com.nonononoki.alovoa.matching.rerank.service.MatchingEventIngestionService;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.MessageDto;
import com.nonononoki.alovoa.model.MessageReactionDto;
import com.nonononoki.alovoa.model.MessageStatusDto;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.MessageReactionRepository;
import com.nonononoki.alovoa.repo.MessageRepository;
import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.rest.ChatWebSocketController;

@Service
public class MessageService {

	@Value("${app.message.size}")
	private int maxMessageSize;

	@Value("${app.conversation.messages-max}")
	private int maxConvoMessages;

	@Autowired
	private AuthService authService;

	@Autowired
	private ConversationRepository conversationRepo;

	@Autowired
	private MessageRepository messageRepo;

	@Autowired
	private MessageReactionRepository messageReactionRepo;

	@Autowired
	private MailService mailService;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private ContentModerationService moderationService;

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private MatchingEventIngestionService matchingEventIngestionService;

	/**
	 * Open messaging entry point (OKCupid 2016 parity).
	 * Creates a conversation if needed, and optionally sends an initial message.
	 */
	public Conversation openConversation(UUID targetUserUuid, String initialMessage)
			throws AlovoaException, GeneralSecurityException, IOException {
		User currentUser = authService.getCurrentUser(true);
		User targetUser = userRepo.findByUuid(targetUserUuid);

		if (targetUser == null) {
			throw new AlovoaException("user_not_found");
		}
		if (currentUser.getId().equals(targetUser.getId())) {
			throw new AlovoaException("user_is_same_user");
		}
		if (isBlocked(currentUser, targetUser) || isBlocked(targetUser, currentUser)) {
			throw new AlovoaException("user_blocked");
		}

		Conversation conversation = conversationRepo.findByUsers(currentUser.getId(), targetUser.getId())
				.orElseGet(() -> createConversation(currentUser, targetUser));

		if (initialMessage != null && !initialMessage.trim().isEmpty()) {
			send(conversation.getId(), initialMessage.trim());
		}

		return conversation;
	}

	public void send(Long convoId, String message)
			throws AlovoaException, GeneralSecurityException, IOException {

		User currUser = authService.getCurrentUser(true);

		Conversation c = conversationRepo.findById(convoId).orElse(null);

		if (c == null) {
			throw new AlovoaException("conversation_not_found");
		}

		if (!c.containsUser(currUser)) {
			throw new AlovoaException("user_not_in_conversation");
		}

		if (message.length() > maxMessageSize) {
			throw new AlovoaException("message_length_too_long");
		}

		// Check content moderation BEFORE saving
		com.nonononoki.alovoa.model.ModerationResult moderationResult =
			moderationService.moderateContent(message, currUser, "MESSAGE");
		if (!moderationResult.isAllowed()) {
			throw new AlovoaException("message_content_blocked");
		}

		User user = c.getPartner(currUser);

		if (user.getBlockedUsers().stream().filter(o -> o.getUserTo().getId() != null)
                .anyMatch(o -> o.getUserTo().getId().equals(currUser.getId()))) {
			throw new AlovoaException("user_blocked");
		}

		if (currUser.getBlockedUsers().stream().filter(o -> o.getUserTo().getId() != null)
                .anyMatch(o -> o.getUserTo().getId().equals(user.getId()))) {
			throw new AlovoaException("user_blocked");
		}
		
		Message m = new Message();
		m.setContent(message);
		m.setConversation(c);
		m.setDate(new Date());
		m.setUserFrom(currUser);  // Current user is the sender
		m.setUserTo(user);        // Partner is the recipient
		
		if(Tools.isURLValid(message)) {
			m.setAllowedFormatting(true);
		}
		c.getMessages().add(m);
		conversationRepo.saveAndFlush(c);

		int numMessages = c.getMessages().size();
		if (numMessages > maxConvoMessages) {
			Message msg = Collections.min(c.getMessages(), Comparator.comparing(Message::getDate));
			c.getMessages().remove(msg);
		}

		c.setLastUpdated(new Date());
		conversationRepo.saveAndFlush(c);
		matchingEventIngestionService.recordMessage(currUser, user, c.getId());

		if(user.getUserSettings().isEmailChat()){
			mailService.sendChatNotificationMail(user, currUser, message, c);
		}

		// Broadcast message via WebSocket to the recipient
		try {
			ChatWebSocketController wsController = applicationContext.getBean(ChatWebSocketController.class);
			MessageDto messageDto = MessageDto.messageToDto(m, user);
			wsController.broadcastMessage(user.getUuid().toString(), messageDto);
		} catch (Exception e) {
			// Log error but don't fail the message send if WebSocket broadcast fails
			// The message is already saved and will be retrieved via polling if needed
		}
	}
	
	public Date updateCheckedDate(Conversation c) throws AlovoaException {
		User user = authService.getCurrentUser(true);
		Date now = new Date();
		Date lastCheckedDate = null;
		if (c.getCheckedDates() == null) {
			c.setCheckedDates(new ArrayList<>());
		}
		ConversationCheckedDate convoCheckedDate = c.getCheckedDates().stream()
				.filter(d -> d.getUserId().equals(user.getId())).findAny().orElse(null);
		if (convoCheckedDate == null) {
			ConversationCheckedDate ccd = new ConversationCheckedDate();
			ccd.setConversation(c);
			ccd.setLastCheckedDate(now);
			ccd.setUserId(user.getId());
			c.getCheckedDates().add(ccd);
		} else {
			c.getCheckedDates().remove(convoCheckedDate);
			lastCheckedDate = convoCheckedDate.getLastCheckedDate();
			convoCheckedDate.setLastCheckedDate(now);
			c.getCheckedDates().add(convoCheckedDate);
		}
		
		conversationRepo.saveAndFlush(c);

		return lastCheckedDate;
	}

	/**
	 * Mark all messages in a conversation as read by the current user
	 */
	public void markConversationAsRead(User user, Long conversationId) throws AlovoaException {
		Conversation conversation = conversationRepo.findById(conversationId).orElse(null);

		if (conversation == null) {
			throw new AlovoaException("conversation_not_found");
		}

		if (!conversation.containsUser(user)) {
			throw new AlovoaException("user_not_in_conversation");
		}

		// Find all unread messages sent TO this user in this conversation
		List<Message> unreadMessages = messageRepo.findUnreadMessagesInConversation(conversation, user);

		if (unreadMessages.isEmpty()) {
			return; // Nothing to mark as read
		}

		Date now = new Date();
		User partner = conversation.getPartner(user);

		// Mark all messages as read
		for (Message message : unreadMessages) {
			message.setReadAt(now);
			// Also mark as delivered if not already
			if (message.getDeliveredAt() == null) {
				message.setDeliveredAt(now);
			}
		}

		messageRepo.saveAll(unreadMessages);

		// Broadcast read receipts to the sender via WebSocket
		try {
			ChatWebSocketController wsController = applicationContext.getBean(ChatWebSocketController.class);
			for (Message message : unreadMessages) {
				MessageStatusDto statusDto = new MessageStatusDto(
					message.getId(),
					conversationId,
					"read",
					now,
					user.getId()
				);
				wsController.broadcastReadReceipt(partner.getUuid().toString(), statusDto);
			}
		} catch (Exception e) {
			// Log error but don't fail if WebSocket broadcast fails
			// The read status is already saved in the database
		}
	}

	/**
	 * Mark messages as delivered when user connects via WebSocket
	 */
	public void markConversationAsDelivered(User user, Long conversationId) throws AlovoaException {
		Conversation conversation = conversationRepo.findById(conversationId).orElse(null);

		if (conversation == null) {
			throw new AlovoaException("conversation_not_found");
		}

		if (!conversation.containsUser(user)) {
			throw new AlovoaException("user_not_in_conversation");
		}

		// Find all undelivered messages sent TO this user
		List<Message> undeliveredMessages = messageRepo.findUndeliveredMessagesInConversation(conversation, user);

		if (undeliveredMessages.isEmpty()) {
			return;
		}

		Date now = new Date();
		User partner = conversation.getPartner(user);

		// Mark all messages as delivered
		for (Message message : undeliveredMessages) {
			message.setDeliveredAt(now);
		}

		messageRepo.saveAll(undeliveredMessages);

		// Broadcast delivery receipts to the sender
		try {
			ChatWebSocketController wsController = applicationContext.getBean(ChatWebSocketController.class);
			for (Message message : undeliveredMessages) {
				MessageStatusDto statusDto = new MessageStatusDto(
					message.getId(),
					conversationId,
					"delivered",
					now,
					user.getId()
				);
				wsController.broadcastReadReceipt(partner.getUuid().toString(), statusDto);
			}
		} catch (Exception e) {
			// Log error but don't fail if WebSocket broadcast fails
		}
	}

	/**
	 * Add or update a reaction to a message
	 * Each user can only have one reaction per message (updating emoji will replace previous)
	 */
	public MessageReactionDto addReaction(User user, Long messageId, String emoji) throws AlovoaException {
		// Validate and get message
		Message message = messageRepo.findById(messageId)
				.orElseThrow(() -> new AlovoaException("message_not_found"));

		Conversation conversation = message.getConversation();

		// Validate user is part of the conversation
		if (!conversation.containsUser(user)) {
			throw new AlovoaException("user_not_in_conversation");
		}

		// Check if user already has a reaction on this message
		MessageReaction reaction = messageReactionRepo.findByMessageAndUser(message, user)
				.orElse(null);

		if (reaction != null) {
			// Update existing reaction
			reaction.setEmoji(emoji);
			reaction.setCreatedAt(new Date());
		} else {
			// Create new reaction
			reaction = new MessageReaction();
			reaction.setMessage(message);
			reaction.setUser(user);
			reaction.setEmoji(emoji);
			reaction.setCreatedAt(new Date());
		}

		reaction = messageReactionRepo.saveAndFlush(reaction);

		MessageReactionDto reactionDto = MessageReactionDto.reactionToDto(reaction);

		// Broadcast reaction update via WebSocket to both users in conversation
		try {
			ChatWebSocketController wsController = applicationContext.getBean(ChatWebSocketController.class);
			User partner = conversation.getPartner(user);

			// Send to partner
			wsController.broadcastReaction(partner.getUuid().toString(), reactionDto);

			// Also send to self for multi-device sync
			wsController.broadcastReaction(user.getUuid().toString(), reactionDto);
		} catch (Exception e) {
			// Log error but don't fail the reaction if WebSocket broadcast fails
		}

		return reactionDto;
	}

	/**
	 * Remove a reaction from a message
	 */
	public void removeReaction(User user, Long messageId) throws AlovoaException {
		// Validate and get message
		Message message = messageRepo.findById(messageId)
				.orElseThrow(() -> new AlovoaException("message_not_found"));

		Conversation conversation = message.getConversation();

		// Validate user is part of the conversation
		if (!conversation.containsUser(user)) {
			throw new AlovoaException("user_not_in_conversation");
		}

		// Find user's reaction
		MessageReaction reaction = messageReactionRepo.findByMessageAndUser(message, user)
				.orElse(null);

		if (reaction == null) {
			// No reaction to remove
			return;
		}

		messageReactionRepo.delete(reaction);

		// Broadcast reaction removal via WebSocket
		try {
			ChatWebSocketController wsController = applicationContext.getBean(ChatWebSocketController.class);
			User partner = conversation.getPartner(user);

			MessageReactionDto reactionDto = new MessageReactionDto();
			reactionDto.setMessageId(messageId);
			reactionDto.setUserId(user.getId());

			// Send to partner
			wsController.broadcastReactionRemoval(partner.getUuid().toString(), reactionDto);

			// Also send to self for multi-device sync
			wsController.broadcastReactionRemoval(user.getUuid().toString(), reactionDto);
		} catch (Exception e) {
			// Log error but don't fail the removal if WebSocket broadcast fails
		}
	}

	/**
	 * Get all reactions for a specific message.
	 * Used by the REST endpoint to fetch fresh reaction data after WebSocket events.
	 */
	public List<MessageReactionDto> getMessageReactions(Long messageId) throws AlovoaException {
		User user = authService.getCurrentUser(true);

		Message message = messageRepo.findById(messageId)
				.orElseThrow(() -> new AlovoaException("message_not_found"));

		Conversation conversation = message.getConversation();

		// Validate user is part of the conversation
		if (!conversation.containsUser(user)) {
			throw new AlovoaException("user_not_in_conversation");
		}

		return MessageReactionDto.reactionsToDtos(message.getReactions());
	}

	private Conversation createConversation(User userA, User userB) {
		Conversation convo = new Conversation();
		convo.setDate(new Date());
		convo.setLastUpdated(new Date());
		convo.setUsers(new ArrayList<>());
		convo.setMessages(new ArrayList<>());
		convo.addUser(userA);
		convo.addUser(userB);
		convo = conversationRepo.saveAndFlush(convo);

		userA.addConversation(convo);
		userB.addConversation(convo);
		userRepo.saveAndFlush(userA);
		userRepo.saveAndFlush(userB);

		return convo;
	}

	private boolean isBlocked(User from, User to) {
		return from.getBlockedUsers().stream()
				.filter(o -> o.getUserTo() != null)
				.anyMatch(o -> o.getUserTo().getId().equals(to.getId()));
	}

}
