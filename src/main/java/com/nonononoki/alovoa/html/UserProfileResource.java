package com.nonononoki.alovoa.html;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.service.AuthService;

@Controller
public class UserProfileResource {
	
	@Autowired
	private UserRepository userRepo;
	
	@Autowired
	private AuthService authService;
	
	@Autowired
	private TextEncryptorConverter textEncryptor;
	
	@Autowired
	private ProfileResource profileResource;
	
	
	@GetMapping("/profile/view/{idEncoded}")
	public ModelAndView profileView(@PathVariable String idEncoded, @RequestParam(required = false) String showHeader) throws NumberFormatException, Exception {
		long id =  UserDto.decodeId(idEncoded, textEncryptor);
		User userView = userRepo.findById(id).orElse(null);
		User user = authService.getCurrentUser();
		
		if(user.getId().equals(id)) {
			return profileResource.profile();
		}
		
		if(userView != null) {		
			if(userView.getBlockedUsers().stream().anyMatch(o -> o.getUserTo().getId().equals(user.getId()))) {
				throw new Exception("blocked");
			}	
			
			userView.setNumberProfileViews(userView.getNumberProfileViews()+1);
			userView = userRepo.saveAndFlush(userView);
			
			ModelAndView mav = new ModelAndView("userProfile");
			mav.addObject("user", UserDto.userToUserDto(userView, user, textEncryptor, UserDto.NO_AUDIO));
			mav.addObject("currUser", UserDto.userToUserDto(user, user, textEncryptor, UserDto.NO_MEDIA));
			boolean hideHeader = false;
			if(showHeader != null && showHeader.equals("false")) {
				hideHeader = true;
			}
			mav.addObject("hideHeader",  hideHeader );

			return mav;
			
		} else {
			throw new Exception("user_not_found");
		}		
	}
}
