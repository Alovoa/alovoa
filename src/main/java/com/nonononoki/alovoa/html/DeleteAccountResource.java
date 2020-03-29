package com.nonononoki.alovoa.html;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/")
public class DeleteAccountResource {
	
	@GetMapping("/delete-account/{tokenString}")
	public ModelAndView deleteAccount(@PathVariable String tokenString) throws Exception {
		ModelAndView mav = new ModelAndView("delete-account");
		mav.addObject("tokenString", tokenString);
		return mav;
	}
}
