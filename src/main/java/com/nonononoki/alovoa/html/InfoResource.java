package com.nonononoki.alovoa.html;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.model.InfoDto;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.UserLikeRepository;
import com.nonononoki.alovoa.repo.UserRepository;

@Controller
public class InfoResource {

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private ConversationRepository conversationRepo;

	@Autowired
	private UserLikeRepository userLikeRepo;

	@GetMapping("/info")
	public ModelAndView imprint() throws JsonProcessingException {

		ModelAndView mav = new ModelAndView("info");
		mav.addObject("info",
				objectMapper.writeValueAsString(InfoDto.builder().numConfirmedUsers(userRepo.countByConfirmed(true))
						.numFemaleUser(userRepo.countByConfirmedAndGenderId(true, Tools.GENDER_FEMALE_ID))
						.numMaleUsers(userRepo.countByConfirmedAndGenderId(true, Tools.GENDER_MALE_ID))
						.numLikes(userLikeRepo.count()).numMatches(conversationRepo.count()).build()));
		return mav;
	}
}
