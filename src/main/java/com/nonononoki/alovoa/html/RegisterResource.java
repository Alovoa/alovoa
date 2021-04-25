package com.nonononoki.alovoa.html;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.repo.GenderRepository;
import com.nonononoki.alovoa.repo.UserIntentionRepository;
import com.nonononoki.alovoa.service.AuthService;
import com.nonononoki.alovoa.service.RegisterService;

@Controller
public class RegisterResource {

	@Autowired
	private RegisterService registerService;

	@Autowired
	private GenderRepository genderRepo;

	@Autowired
	private UserIntentionRepository userIntentionRepo;
	
	@Autowired
	private ProfileResource profileResource;
	
	@Autowired
	private AuthService authService;
	
	@GetMapping("/register")
	public ModelAndView register() throws Exception {
		
		User user = authService.getCurrentUser();
		if (user != null) {
			return profileResource.profile();
		}
		ModelAndView mav = new ModelAndView("register");
		mav.addObject("genders", genderRepo.findAll());
		mav.addObject("intentions", userIntentionRepo.findAll());
		return mav;
	}
	
	public ModelAndView registerOauth(User user) throws Exception {
		ModelAndView mav = new ModelAndView("register-oauth");
		mav.addObject("genders", genderRepo.findAll());
		mav.addObject("intentions", userIntentionRepo.findAll());
		return mav;
	}

	@GetMapping("/register/confirm/{tokenString}")
	public String registerConfirm(@PathVariable String tokenString) {
		try {
			registerService.registerConfirm(tokenString);
			return "redirect:/?registration-confirm-success";
		} catch (Exception e) {
			return "redirect:/?registration-confirm-failed";
		}

	}
}
