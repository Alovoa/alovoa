package com.nonononoki.alovoa.html;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class DonateListResource {

	@GetMapping("/donate-list")
	public ModelAndView donate(@RequestParam(required = false) String showHeader) throws Exception {
		ModelAndView mav = new ModelAndView("donate-list");
		boolean hideHeader = false;
		if (showHeader != null && showHeader.equals("false")) {
			hideHeader = true;
		}
		mav.addObject("hideHeader", hideHeader);
		return mav;
	}
}
