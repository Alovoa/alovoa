package com.nonononoki.alovoa.rest;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Date;

import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.Conversation;
import com.nonononoki.alovoa.entity.user.ConversationCheckedDate;
import com.nonononoki.alovoa.model.AlovoaException;
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
	public void send(@RequestBody String msg, @PathVariable long convoId)
			throws AlovoaException, GeneralSecurityException, IOException, JoseException {
		messageService.send(convoId, msg);
	}

	@GetMapping(value = "/get-messages/{convoId}/{first}")
	public String getMessages(Model model, @PathVariable long convoId, @PathVariable int first) throws AlovoaException {

		model = getMessagesModel(model, convoId, first);
		boolean show = (boolean) model.getAttribute("show");
		if (show) {
			return "fragments :: message-detail";
		} else {
			return "fragments :: empty";
		}
	}

	public Model getMessagesModel(Model model, long convoId, int first) throws AlovoaException {
		User user = authService.getCurrentUser(true);
		Conversation c = conversationRepo.findById(convoId).orElse(null);

		if (c == null) {
			throw new AlovoaException("conversation_not_found");
		}

		if (!c.containsUser(user)) {
			throw new AlovoaException("user_not_in_conversation");
		}

		User partner = c.getPartner(user);

		if (user.getBlockedUsers().stream().anyMatch(o -> o.getUserTo().getId().equals(partner.getId()))) {
			throw new AlovoaException("user_blocked");
		}

		if (partner.getBlockedUsers().stream().anyMatch(o -> o.getUserTo().getId().equals(user.getId()))) {
			throw new AlovoaException("user_blocked");
		}

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

		if(model == null) {
			model = new ConcurrentModel();
		}
		model.addAttribute("messages", MessageDto.messagesToDtos(c.getMessages(), user));
		boolean show = first == 1 || lastCheckedDate == null || !lastCheckedDate.after(c.getLastUpdated());
		model.addAttribute("show", show);
		return model;
	}
}
