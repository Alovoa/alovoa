package com.nonononoki.alovoa.html;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.repo.GenderRepository;
import com.nonononoki.alovoa.repo.UserIntentionRepository;
import com.nonononoki.alovoa.service.AuthService;

@Controller
public class ProfileOnboardingResource {

	@Autowired
	private AuthService authService;

	@Autowired
	private GenderRepository genderRepo;

	@Autowired
	private UserIntentionRepository userIntentionRepo;
	@Value("${app.media.max-size}")
	private int mediaMaxSize;

	@Value("${app.interest.max}")
	private int interestMaxSize;
	
	public static final String URL = "/user/onboarding";

	@GetMapping(URL)
	public ModelAndView onboarding() throws AlovoaException {

		User user = authService.getCurrentUser(true);
		if (user.isAdmin()) {
			return new ModelAndView("redirect:" + AdminResource.URL);
		} else if (user.getProfilePicture() != null || user.getDescription() != null) {
			return new ModelAndView("redirect:" + ProfileResource.URL);
		} else {
			int age = Tools.calcUserAge(user);
			boolean isLegal = age >= Tools.AGE_LEGAL;
			ModelAndView mav = new ModelAndView("profile-onboarding");
			mav.addObject("genders", genderRepo.findAll());
			mav.addObject("intentions", userIntentionRepo.findAll());
			mav.addObject("isLegal", isLegal);
			mav.addObject("mediaMaxSize", mediaMaxSize);
			mav.addObject("interestMaxSize", interestMaxSize);
			return mav;
		}
	}
}
