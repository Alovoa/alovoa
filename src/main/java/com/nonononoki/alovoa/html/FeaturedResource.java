package com.nonononoki.alovoa.html;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import com.nonononoki.alovoa.service.CaptchaService;

@Controller
public class FeaturedResource {

	@Autowired
	private CaptchaService captchaService;
	
	@GetMapping("/featured")
	public ModelAndView register() {

		ModelAndView mav = new ModelAndView("register");
		mav.addObject("captcha", captchaService.generate());
		return mav;
	}
}
