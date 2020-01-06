package com.nonononoki.alovoa.html;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class LoginResource {

	@GetMapping("/login")
	public ModelAndView login() {

		ModelAndView mav = new ModelAndView("login");
		return mav;
	}
}
