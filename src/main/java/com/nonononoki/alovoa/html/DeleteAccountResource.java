package com.nonononoki.alovoa.html;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserDeleteToken;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.service.AuthService;

@Controller
@RequestMapping("/")
public class DeleteAccountResource {
	
	@Autowired
	private AuthService authService;
	
	@Autowired
	private TextEncryptorConverter textEncryptor; 
	
	@GetMapping("/delete-account/{tokenString}")
	public ModelAndView deleteAccount(@PathVariable String tokenString) throws Exception {
		User user = authService.getCurrentUser();
		ModelAndView mav = new ModelAndView("delete-account");
		mav.addObject("tokenString", tokenString);
		mav.addObject("user",  UserDto.userToUserDto(user, user, textEncryptor, UserDto.NO_MEDIA));
		
		UserDeleteToken token = user.getDeleteToken();
		boolean active = false;
		long ms = new Date().getTime();
		if(token != null && token.getActiveDate().getTime() < ms) {
			active = true;
		}	
		mav.addObject("active",  active);
		
		return mav;
	}
}
