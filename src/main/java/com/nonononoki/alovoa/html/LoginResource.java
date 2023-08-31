package com.nonononoki.alovoa.html;

import org.jboss.logging.Logger;
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
	private static final Logger logger = Logger.getLogger(LoginResource.class);

	private boolean facebookEnabled;
	private boolean googleEnabled;
	private boolean ip6liEnabled;

	@Autowired
	private AuthService authService;

	@Value("${app.privacy.update-date}")
	private String privacyDate;

	@Value("${app.captcha.enabled}")
	private String captchaEnabled;

	@Value("${spring.security.oauth2.client.registration.facebook.client-id:#{null}}")
	private void facebookEnabled(String clientId) { this.facebookEnabled = clientId!=null; }

	@Value("${spring.security.oauth2.client.registration.google.client-id:#{null}}")
	private void googleEnabled(String clientId) { this.googleEnabled = clientId!=null; }

	@Value("${spring.security.oauth2.client.registration.ip6li.client-id:#{null}}")
	private void ip6liEnabled(String clientId) { this.ip6liEnabled = clientId!=null; }

	public static final String URL = "/login";

	@GetMapping(URL)
	public ModelAndView login() throws AlovoaException {

		User user = authService.getCurrentUser();
		if (user != null) {
			return new ModelAndView("redirect:" + SearchResource.URL);
		}

		ModelAndView mav = new ModelAndView("login");
		mav.addObject("captchaEnabled", Boolean.valueOf(captchaEnabled));
		mav.addObject("facebookEnabled", facebookEnabled);
		mav.addObject("googleEnabled", googleEnabled);
		mav.addObject("ip6liEnabled", ip6liEnabled);
		logger.trace(String.format("Captcha enabled: %s", captchaEnabled));
		return mav;
	}
}
