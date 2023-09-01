package com.nonononoki.alovoa.html;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class DonateListResource {

	@Value("${donate.enabled}")
	private String donateEnabled;

	@GetMapping("/donate-list")
	public ModelAndView donateList() {
		ModelAndView mav = new ModelAndView("donate-list");
		mav.addObject("donateEnabled", Boolean.parseBoolean(donateEnabled));
		return mav;
	}
}
