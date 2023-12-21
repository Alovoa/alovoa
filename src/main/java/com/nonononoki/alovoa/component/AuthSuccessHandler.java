package com.nonononoki.alovoa.component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.config.SecurityConfig;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.service.AuthService;

@Component
public class AuthSuccessHandler implements AuthenticationSuccessHandler {

	@Value("${app.url.auth.success}")
	private String url;

	private SecurityConfig securityConfig;
	
	@Autowired
	private AuthService authService;
	
	private static final int PAGE_ONBOARDING = 2;
	private static final int PAGE_DEFAULT = 3;

	public AuthSuccessHandler(SecurityConfig securityConfig) {
		super();
		this.securityConfig = securityConfig;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		response.setStatus(HttpServletResponse.SC_OK);
		HttpSession httpSession = request.getSession();
		if (httpSession.getAttribute(AuthFilter.REDIRECT_URL) != null) {
			String redirectUrl = new String(
					Base64.getDecoder().decode((String) request.getSession().getAttribute(AuthFilter.REDIRECT_URL)),
					StandardCharsets.UTF_8);
			User user;
			try {
				user = authService.getCurrentUser(true);
				int page = PAGE_DEFAULT;
				if(user.getProfilePicture() == null && user.getDescription() == null) {
					page = PAGE_ONBOARDING;
				}
				redirectUrl = redirectUrl + Tools.getAuthParams(securityConfig, httpSession.getId(), user.getEmail(), user.getFirstName(), page, user.getPassword());
				response.setHeader(AuthFilter.REDIRECT_URL, redirectUrl);
				//sometimes(?) the header is read-only, so, just add the redirectUrl as body as fallback
				response.getWriter().write(redirectUrl);
				response.getWriter().flush();
				return;
			} catch (AlovoaException e) {
				e.printStackTrace();
			}
		}
		response.sendRedirect(url);
	}

}
