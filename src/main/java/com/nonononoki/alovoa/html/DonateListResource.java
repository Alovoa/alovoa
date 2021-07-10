package com.nonononoki.alovoa.html;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class DonateListResource {

	@GetMapping("/donate-list")
	public ModelAndView donateList() {
		ModelAndView mav = new ModelAndView("donate-list");
		return mav;
	}
}
