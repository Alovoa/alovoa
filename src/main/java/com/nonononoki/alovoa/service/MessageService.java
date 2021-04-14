package com.nonononoki.alovoa.service;

import java.util.Date;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.Conversation;
import com.nonononoki.alovoa.entity.user.Message;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.MessageRepository;

@Service
public class MessageService {

	@Value("${app.message.size}")
	private int maxMessageSize;
	
	@Autowired
	private AuthService authService;
	
	@Autowired
	private MessageRepository messageRepo;
	
	@Autowired
	private ConversationRepository conversationRepo;
	
	@Autowired
	private NotificationService notificationService;
	
	@Autowired
	private MessageSource messageSource;
	
	private final String URL_PREFIX = "<a href=\"";
	
	private final String URL_JITSI = "https://meet.jit.si";
	
	public void send(Conversation c, String message) throws Exception {
		
		if(message.length() > maxMessageSize) {
			throw new Exception("message_length_too_long");	
		}
		
		User currUser = authService.getCurrentUser();
		User user = c.getPartner(currUser);
		
		if(user.getBlockedUsers().stream().anyMatch(o -> o.getUserTo().getId().equals(currUser.getId()))) {
			throw new Exception();
		}
		
		if(currUser.getBlockedUsers().stream().anyMatch(o -> o.getUserTo().getId().equals(user.getId()))) {
			throw new Exception();
		}
		
		String lastMessage = message;
		boolean allowedFormatting = false;
		if(message.startsWith(URL_PREFIX + URL_JITSI)) {
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
		messageRepo.save(m);
		
		c.setLastMessage(lastMessage);
		c.setLastUpdated(new Date());
		conversationRepo.saveAndFlush(c);
		
		notificationService.newMessage(user);
	}

}
