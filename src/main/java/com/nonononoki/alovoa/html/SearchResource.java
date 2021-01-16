package com.nonononoki.alovoa.html;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.service.AuthService;

@Controller
public class SearchResource {
		
	@Autowired
	private AuthService authService;
	
	@Autowired
	private TextEncryptorConverter textEncryptor;
	
	@GetMapping("/search")
	public ModelAndView search() throws Exception {
		User user = authService.getCurrentUser();
		ModelAndView mav = new ModelAndView("search");
		mav.addObject("user", UserDto.userToUserDto(user, user, textEncryptor, UserDto.NO_MEDIA));
		return mav;
	}
}
