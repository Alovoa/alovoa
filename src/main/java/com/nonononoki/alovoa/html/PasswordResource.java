package com.nonononoki.alovoa.html;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.service.AuthService;

@Controller
@RequestMapping("/password")
public class PasswordResource {
	
	@Autowired
	private AuthService authService;
	
	@Autowired
	private TextEncryptorConverter textEncryptor;
	
	@GetMapping("/reset")
	public ModelAndView passwordReset() throws Exception {
		ModelAndView mav = new ModelAndView("password-reset");
		User user = authService.getCurrentUser();
		mav.addObject("user", UserDto.userToUserDto(user, user, textEncryptor, UserDto.NO_MEDIA));
		return mav;
	}

	@GetMapping("/change/{tokenString}")
	public ModelAndView passwordChange(@PathVariable String tokenString) throws Exception {
		ModelAndView mav = new ModelAndView("password-change");
		mav.addObject("tokenString", tokenString);
		User user = authService.getCurrentUser();
		mav.addObject("user", UserDto.userToUserDto(user, user, textEncryptor, UserDto.NO_MEDIA));
		return mav;
	}
}
