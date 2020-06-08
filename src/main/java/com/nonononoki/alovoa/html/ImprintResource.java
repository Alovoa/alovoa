package com.nonononoki.alovoa.html;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ImprintResource {

	@GetMapping("/imprint")
	public ModelAndView imprint() {

		ModelAndView mav = new ModelAndView("imprint");
		return mav;
	}
}
