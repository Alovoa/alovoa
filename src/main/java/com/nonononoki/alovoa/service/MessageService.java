package com.nonononoki.alovoa.service;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.entity.Conversation;
import com.nonononoki.alovoa.entity.Message;
import com.nonononoki.alovoa.entity.User;
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
	
	public void send(Conversation c, String message) throws Exception {
		if(message.length() > maxMessageSize) {
			throw new Exception("");	
		}
		
		User user = authService.getCurrentUser();
		
		Message m = new Message();
		m.setContent(message);
		m.setConversation(c);
		m.setCreationDate(new Date());
		m.setUserFrom(user);
		m.setUserTo(c.getPartner(user));
		messageRepo.save(m);
		
		c.setLastMessage(message);
		c.setLastUpdated(new Date());
		conversationRepo.saveAndFlush(c);
	}

}
