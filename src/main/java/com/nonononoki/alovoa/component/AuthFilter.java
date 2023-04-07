package com.nonononoki.alovoa.component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.nonononoki.alovoa.model.AuthToken;

public class AuthFilter extends UsernamePasswordAuthenticationFilter {

	private static final String USERNAME = "username";
	private static final String PASSWORD = "password";
	private static final String CAPTCHA_ID = "captchaId";
	private static final String CAPTCHA_TEXT = "captchaText";
	public static final String REDIRECT_URL = "redirect-url";
	
	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException {

		String username = request.getParameter(USERNAME);
		String password = request.getParameter(PASSWORD);
		long captchaId = Long.parseLong(request.getParameter(CAPTCHA_ID));
		String captchaText = request.getParameter(CAPTCHA_TEXT);
		request.getSession().setAttribute(REDIRECT_URL, request.getParameter(REDIRECT_URL));
		
		AuthToken auth = new AuthToken(username, password, captchaId, captchaText);
		AuthenticationManager am = this.getAuthenticationManager();
		return am.authenticate(auth);
	}
}
