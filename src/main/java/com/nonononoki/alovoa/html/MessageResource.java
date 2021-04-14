package com.nonononoki.alovoa.html;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.Conversation;
import com.nonononoki.alovoa.entity.user.Message;
import com.nonononoki.alovoa.model.ConversationDto;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.UserBlockRepository;
import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.service.AuthService;

@Controller
public class MessageResource {

	@Autowired
	private AuthService authService;

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private ConversationRepository conversationRepo;
	
	@Autowired
	private UserBlockRepository userBlockRepo;

	@Autowired
	private TextEncryptorConverter textEncryptor;

	@GetMapping("/chats")
	public ModelAndView chats() throws Exception {

		ModelAndView mav = new ModelAndView("messages");
		User user = authService.getCurrentUser();
		user.getDates().setMessageCheckedDate(new Date());
		userRepo.saveAndFlush(user);
		List<ConversationDto> convos = new ArrayList<>();
		for (int i = 0; i < user.getConversations().size(); i++) {
			Conversation c = user.getConversations().get(i);
			if(!c.isBlocked(userBlockRepo)) {
				convos.add(ConversationDto.conversationToDto(c, user, textEncryptor));
			}
		}
		for (int i = 0; i < user.getConversationsBy().size(); i++) {
			Conversation c = user.getConversationsBy().get(i);
			if(!c.isBlocked(userBlockRepo)) {
				convos.add(ConversationDto.conversationToDto(c, user, textEncryptor));
			}
		}

		Collections.sort(convos, new Comparator<ConversationDto>() {
			@Override
			public int compare(ConversationDto a, ConversationDto b) {
				return b.getLastUpdated().compareTo(a.getLastUpdated());
			}
		});
		mav.addObject("conversations", convos);
		mav.addObject("user", UserDto.userToUserDto(user, user, textEncryptor, UserDto.NO_MEDIA));
		return mav;
	}

	@GetMapping("/chats/{id}")
	public ModelAndView chatsDetail(@PathVariable long id) throws Exception {

		ModelAndView mav = new ModelAndView("message-detail");
		User user = authService.getCurrentUser();
		Conversation c = conversationRepo.findById(id).orElse(null);
		if(!c.containsUser(user)) {
			throw new Exception("user_not_in_conversation");
		}
		
		User u = c.getPartner(user);
		
		List<Message> messages = c.getMessages();
		Collections.sort(messages, new Comparator<Message>() {
			@Override
			public int compare(Message a, Message b) {
				return b.getDate().compareTo(a.getDate());
			}
		});
		mav.addObject("user", UserDto.userToUserDto(user, user, textEncryptor, UserDto.NO_MEDIA));
		mav.addObject("messages", messages);
		mav.addObject("convoId", id);
		mav.addObject("partner", UserDto.userToUserDto(u, user, textEncryptor, UserDto.PROFILE_PICTURE_ONLY));
		
		c.setLastOpened(new Date());
		conversationRepo.saveAndFlush(c);
		return mav;
	}
}
