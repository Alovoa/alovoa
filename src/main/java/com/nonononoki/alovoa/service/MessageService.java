package com.nonononoki.alovoa.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.Conversation;
import com.nonononoki.alovoa.entity.user.ConversationCheckedDate;
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
	private MailService mailService;

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
		m.setUserFrom(user);
		m.setUserTo(c.getPartner(user));
		
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

		if(user.getUserSettings().isEmailChat()){
			mailService.sendChatNotificationMail(user, currUser, message, c);
		}
	}
	
	public Date updateCheckedDate(Conversation c) throws AlovoaException {
		User user = authService.getCurrentUser(true);
		Date now = new Date();
		Date lastCheckedDate = null;
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

}
