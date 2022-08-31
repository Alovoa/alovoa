package com.nonononoki.alovoa.html;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class FaqResource {

	@GetMapping("/faq")
	public ModelAndView faq() {
		ModelAndView mav = new ModelAndView("faq");
		return mav;
	}
}
