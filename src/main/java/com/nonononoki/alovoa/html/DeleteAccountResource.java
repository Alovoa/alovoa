package com.nonononoki.alovoa.html;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.service.AuthService;

@Controller
@RequestMapping("/")
public class DeleteAccountResource {
	
	@Autowired
	private AuthService authService;
	
	@GetMapping("/delete-account/{tokenString}")
	public ModelAndView deleteAccount(@PathVariable String tokenString) throws Exception {
		User user = authService.getCurrentUser();
		ModelAndView mav = new ModelAndView("delete-account");
		mav.addObject("tokenString", tokenString);
		mav.addObject("user", user);
		return mav;
	}
}
