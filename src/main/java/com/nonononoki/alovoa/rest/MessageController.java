package com.nonononoki.alovoa.rest;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.Conversation;
import com.nonononoki.alovoa.model.MessageDto;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.service.AuthService;
import com.nonononoki.alovoa.service.MessageService;

@Controller
@RequestMapping("/message")
public class MessageController {

	@Autowired
	private ConversationRepository conversationRepo;

	@Autowired
	private AuthService authService;

	@Autowired
	private MessageService messageService;

	@ResponseBody
	@PostMapping(value = "/send/{convoId}", consumes = "text/plain")
	public void send(@RequestBody String msg, @PathVariable long convoId) throws Exception {
		User user = authService.getCurrentUser();
		Conversation c = conversationRepo.findById(convoId).orElse(null);
		if (!c.containsUser(user)) {
			throw new Exception("");
		}
		messageService.send(c, msg);
	}

	@GetMapping(value = "/get-messages/{convoId}/{first}")
	public String getMessages(Model model, @PathVariable long convoId, @PathVariable int first) throws Exception {
		User user = authService.getCurrentUser();
		Conversation c = conversationRepo.findById(convoId).orElse(null);
		if (!c.containsUser(user)) {
			throw new Exception("");
		}

		Date now = new Date();

		Date lastCheckedDate;
		if (user.equals(c.getUserFrom())) {
			lastCheckedDate = c.getLastCheckedFrom();
			c.setLastCheckedFrom(now);
		} else {
			lastCheckedDate = c.getLastCheckedTo();
			c.setLastCheckedTo(now);
		}

		conversationRepo.saveAndFlush(c);

		if (first == 1 || lastCheckedDate == null || !lastCheckedDate.after(c.getLastUpdated())) {
			model.addAttribute("messages", MessageDto.messagesToDtos(c.getMessages(), user));
			return "fragments :: message-detail";
		}

		return "fragments :: empty";

	}
}
