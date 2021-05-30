package com.nonononoki.alovoa.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.Conversation;
import com.nonononoki.alovoa.entity.user.Message;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.repo.ConversationRepository;

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
	private NotificationService notificationService;

	@Autowired
	private MessageSource messageSource;

	private static final String URL_PREFIX = "<a href=\"";

	private static final String URL_JITSI = "https://meet.jit.si";

	public void send(Long convoId, String message)
			throws AlovoaException, GeneralSecurityException, IOException, JoseException {

		User currUser = authService.getCurrentUser();

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

		User user = c.getPartner(currUser);

		if (user.getBlockedUsers().stream().anyMatch(o -> o.getUserTo().getId().equals(currUser.getId()))) {
			throw new AlovoaException("user_blocked");
		}

		if (currUser.getBlockedUsers().stream().anyMatch(o -> o.getUserTo().getId().equals(user.getId()))) {
			throw new AlovoaException("user_blocked");
		}

		String lastMessage = message;
		boolean allowedFormatting = false;
		if (message.startsWith(URL_PREFIX + URL_JITSI)) {
			allowedFormatting = true;
			Locale locale = LocaleContextHolder.getLocale();
			lastMessage = messageSource.getMessage("message.video-chat", null, locale);
		}

		Message m = new Message();
		m.setContent(message);
		m.setConversation(c);
		m.setDate(new Date());
		m.setUserFrom(user);
		m.setUserTo(c.getPartner(user));
		m.setAllowedFormatting(allowedFormatting);
		c.getMessages().add(m);
		conversationRepo.save(c);

		int numMessages = c.getMessages().size();
		if (numMessages > maxConvoMessages) {
			Message msg = Collections.min(c.getMessages(), Comparator.comparing(Message::getDate));
			c.getMessages().remove(msg);
		}

		c.setLastMessage(lastMessage);
		c.setLastUpdated(new Date());
		conversationRepo.saveAndFlush(c);

		notificationService.newMessage(user);
	}

}
