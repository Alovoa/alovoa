package com.nonononoki.alovoa.html;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.service.AuthService;

@Controller
public class LoginResource {

	@Autowired
	private AuthService authService;

	@Value("${app.privacy.update-date}")
	private String privacyDate;

	@Value("${app.captcha.login.enabled}")
	private String captchaEnabled;

	public static final String URL = "/login";

	@GetMapping(URL)
	public ModelAndView login() throws AlovoaException {

		User user = authService.getCurrentUser();
		if (user != null) {
			return new ModelAndView("redirect:" + SearchResource.URL);
		}

		ModelAndView mav = new ModelAndView("login");
		mav.addObject("captchaEnabled", Boolean.valueOf(captchaEnabled));
		return mav;
	}
}
