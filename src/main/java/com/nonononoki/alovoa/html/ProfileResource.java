package com.nonononoki.alovoa.html;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import com.nonononoki.alovoa.entity.User;
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
	
	@Value("${app.profile.image.max}")
	private int imageMax;

	@GetMapping("/profile")
	public ModelAndView profile() {
		ModelAndView mav = new ModelAndView("profile");
		mav.addObject("user", authService.getCurrentUser());
		mav.addObject("genders", genderRepo.findAll());
		mav.addObject("intentions", userIntentionRepo.findAll());
		mav.addObject("imageMax", imageMax);
		return mav;
	}

	@GetMapping("/profile/warning")
	public String warning(Model model) {

		boolean hasWarning = false;
		boolean noProfilePicture = false;
		boolean noDescription = false;
		boolean noIntention = false;
		boolean noGender = false;
		boolean noLocation = false;

		User user = authService.getCurrentUser();
		// if(user.getProfilePicture() == null || user.getProfilePicture() != null &&
		// user.getProfilePicture().isEmpty()) {
		if (user.getProfilePicture() == null) {
			noProfilePicture = true;
			hasWarning = true;
		}
		if (user.getDescription() == null) {
			noDescription = true;
			hasWarning = true;
		} else if (user.getDescription().isEmpty()) {
			noDescription = true;
			hasWarning = true;
		}
		if (user.getIntention() == null) {
			noIntention = true;
			hasWarning = true;
		}
		if (user.getPreferedGenders() == null) {
			noGender = true;
			hasWarning = true;
		} else if (user.getPreferedGenders().size() == 0) {
			noGender = true;
			hasWarning = true;
		}
		if(user.getLastLocation() == null) {
			noLocation = true;
			hasWarning = true;
		}

		model.addAttribute("hasWarning", hasWarning);
		model.addAttribute("noProfilePicture", noProfilePicture);
		model.addAttribute("noDescription", noDescription);
		model.addAttribute("noIntention", noIntention);
		model.addAttribute("noGender", noGender);
		model.addAttribute("noLocation", noLocation);
		return "fragments :: profile-warning";
	}
}
