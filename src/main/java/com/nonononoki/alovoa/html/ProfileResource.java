package com.nonononoki.alovoa.html;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import com.nonononoki.alovoa.repo.GenderRepository;
import com.nonononoki.alovoa.repo.UserIntentionRepository;
import com.nonononoki.alovoa.service.AuthService;

@Controller
public class ProfileResource {
	
	@Autowired
	private AuthService authService;
	
	@Autowired
	private GenderRepository genderRepo;
	
	@Autowired
	private UserIntentionRepository userIntentionRepo;
	
	@GetMapping("/profile")
	public ModelAndView profile() {

		ModelAndView mav = new ModelAndView("profile");
		mav.addObject("user", authService.getCurrentUser());
		mav.addObject("genders", genderRepo.findAll());
		mav.addObject("intentions", userIntentionRepo.findAll());
		return mav;
	}
}
