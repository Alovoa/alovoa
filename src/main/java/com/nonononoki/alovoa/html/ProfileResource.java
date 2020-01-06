package com.nonononoki.alovoa.html;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.service.AuthService;

@Controller
public class ProfileResource {
	
	@Autowired
	private AuthService authService;
	
	@Autowired
	private UserRepository userRepo;
	
	@GetMapping("/profile")
	public ModelAndView profile() {

		ModelAndView mav = new ModelAndView("profile");
		mav.addObject("user", userRepo.findById(authService.getCurrentUserId()));
		return mav;
	}
}
