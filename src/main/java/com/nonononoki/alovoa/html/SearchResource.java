package com.nonononoki.alovoa.html;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.service.AuthService;

@Controller
public class SearchResource {
		
	@Autowired
	private AuthService authService;
	
	@GetMapping("/search")
	public ModelAndView search() {
		User currUser = authService.getCurrentUser();
		ModelAndView mav = new ModelAndView("search");
		mav.addObject("user", currUser);
		return mav;
	}
}
