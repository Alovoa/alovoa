package com.nonononoki.alovoa.html;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class TermsConditionsResource {
	
	@GetMapping("/terms-conditions")
	public ModelAndView termsConditions() {
		ModelAndView mav = new ModelAndView("terms-conditions");
		return mav;
	}
}
