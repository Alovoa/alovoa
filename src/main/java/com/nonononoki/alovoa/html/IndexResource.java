package com.nonononoki.alovoa.html;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class IndexResource {
	
	@GetMapping("/")
	public ModelAndView register() {

		ModelAndView mav = new ModelAndView("index");
		return mav;
	}
}
