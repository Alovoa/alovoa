package com.nonononoki.alovoa.html;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.UserDto;
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

	@Autowired
	private AdminResource adminResource;
	
	@Autowired
	private TextEncryptorConverter textEncryptor;

	@Value("${app.profile.image.max}")
	private int imageMax;
	
	@Value("${app.vapid.public}")
	private String vapidPublicKey;
	
	@Value("${app.age.legal}")
	private int ageLegal;
	
	@Value("${app.media.max-size}")
	private int mediaMaxSize;
	
	@Value("${app.intention.delay}")
	private long intentionDelay;

	@GetMapping("/profile")
	public ModelAndView profile() throws Exception {

		User user = authService.getCurrentUser();
		if (user.isAdmin()) {
			return adminResource.admin();
		} else {
			int age = Tools.calcUserAge(user);
			boolean isLegal = age >= ageLegal;
			ModelAndView mav = new ModelAndView("profile");
			mav.addObject("user", UserDto.userToUserDto(user, user, textEncryptor, UserDto.ALL));
			mav.addObject("genders", genderRepo.findAll());
			mav.addObject("intentions", userIntentionRepo.findAll());
			mav.addObject("imageMax", imageMax);
			mav.addObject("vapidPublicKey", vapidPublicKey);
			mav.addObject("isLegal", isLegal);
			mav.addObject("mediaMaxSize", mediaMaxSize);
			
			boolean showIntention = false;
			Date now = new Date();
			if(user.getDates().getIntentionChangeDate() == null || 
					now.getTime() >= user.getDates().getIntentionChangeDate().getTime() + intentionDelay ) {
				showIntention = true;
			}
			mav.addObject("showIntention", showIntention);
			
			return mav;
		}
	}

	@GetMapping("/profile/warning")
	public String warning(Model model) throws Exception {

		boolean hasWarning = false;
		boolean noProfilePicture = false;
		boolean noDescription = false;
		boolean noIntention = false;
		boolean noGender = false;
		boolean noLocation = false;

		User user = authService.getCurrentUser();
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
		if (user.getLocationLatitude() == null) {
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
