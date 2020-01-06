package com.nonononoki.alovoa.html;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class RadarResource {
	
	@GetMapping("/radar")
	public ModelAndView radar() {
		ModelAndView mav = new ModelAndView("radar");
		return mav;
	}
}
