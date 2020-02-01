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

import com.nonononoki.alovoa.entity.Conversation;
import com.nonononoki.alovoa.entity.Message;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.repo.ConversationRepository;
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

	@GetMapping("/chats")
	public ModelAndView chats() throws Exception {

		ModelAndView mav = new ModelAndView("messages");
		User user = authService.getCurrentUser();
		user.setMessageCheckedDate(new Date());
		userRepo.saveAndFlush(user);
		List<Conversation> convos = new ArrayList<>();
		Collections.sort(convos, new Comparator<Conversation>() {
			@Override
			public int compare(Conversation a, Conversation b) {
				return b.getLastUpdated().compareTo(a.getLastUpdated());
			}
		});
		mav.addObject("conversations", convos);
		mav.addObject("user", user);
		return mav;
	}
	
	@GetMapping("/chats/{id}")
	public ModelAndView chatsDetail(@PathVariable long id) throws Exception {

		ModelAndView mav = new ModelAndView("messageDetail");
		User user = authService.getCurrentUser();
		Conversation c = conversationRepo.findById(id).orElse(null);
		List<Message> messages = c.getMessages();
		mav.addObject("user", user);
		mav.addObject("messages", messages);
		return mav;
	}
}
