package com.nonononoki.alovoa.html;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/password")
public class PasswordResource {
	
	@GetMapping("/reset")
	public ModelAndView passwordReset() throws Exception {
		ModelAndView mav = new ModelAndView("password-reset");
		return mav;
	}

	@GetMapping("/change/{tokenString}")
	public ModelAndView passwordChange(@PathVariable String tokenString) throws Exception {
		ModelAndView mav = new ModelAndView("password-change");
		mav.addObject("tokenString", tokenString);
		return mav;
	}
}
