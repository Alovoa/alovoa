package com.nonononoki.alovoa.html;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ImprintResource {
	
	@Value("${app.company.name}")
	private String companyName;

	@GetMapping("/imprint")
	public ModelAndView imprint() {

		ModelAndView mav = new ModelAndView("imprint");
		mav.addObject("companyName", companyName);
		return mav;
	}
}
