package com.nonononoki.alovoa.html;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class SearchResource {
		
	@GetMapping("/search")
	public ModelAndView search() {
		ModelAndView mav = new ModelAndView("search");
		return mav;
	}
}
