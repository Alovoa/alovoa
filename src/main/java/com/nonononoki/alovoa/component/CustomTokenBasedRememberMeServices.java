package com.nonononoki.alovoa.component;

import java.util.Date;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.util.StringUtils;

public class CustomTokenBasedRememberMeServices extends TokenBasedRememberMeServices {
	
	public static final String COOKIE_REMEMBER = "remember-me";
	public static final String COOKIE_REMEMBER_EXPIRE = "remember-me-expire";

	public CustomTokenBasedRememberMeServices(String key, UserDetailsService userDetailsService) {
		super(key, userDetailsService);
	}

	@Override
	protected String retrieveUserName(Authentication authentication) {
		if (isInstanceOfUserDetails(authentication)) {
			return ((UserDetails) authentication.getPrincipal()).getUsername();
		}
		return (String) ((DefaultOAuth2User) authentication.getPrincipal()).getAttributes().get("email");
	}

	// Mostly copied from TokenBasedRememberMeServices
	@Override
	public void onLoginSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication successfulAuthentication) {
		String username = retrieveUserName(successfulAuthentication);
		String password = retrievePassword(successfulAuthentication);
		// If unable to find a username and password, just abort as
		// TokenBasedRememberMeServices is
		// unable to construct a valid token in this case.
		if (!StringUtils.hasLength(username)) {
			this.logger.debug("Unable to retrieve username");
			return;
		}
		if (!StringUtils.hasLength(password)) {
			try {
				UserDetails user = getUserDetailsService().loadUserByUsername(username);
				password = user.getPassword();
			} catch (UsernameNotFoundException e) {
				password = null;
			}
		}
		int tokenLifetime = TWO_WEEKS_S;
		long expiryTime = System.currentTimeMillis();
		// SEC-949
		expiryTime += 1000L * TWO_WEEKS_S;
		String signatureValue = makeTokenSignature(expiryTime, username, password);
		setCookie(new String[] { username, Long.toString(expiryTime), signatureValue }, tokenLifetime, request,
				response);
		if (this.logger.isDebugEnabled()) {
			this.logger.debug(
					"Added remember-me cookie for user '" + username + "', expiry: '" + new Date(expiryTime) + "'");
		}
	}
	
	// Mostly copied from TokenBasedRememberMeServices
	public Cookie getRememberMeCookie(String cookieValue, HttpServletRequest request, HttpServletResponse response) {
		Cookie cookie = new Cookie(COOKIE_REMEMBER, cookieValue);
		cookie.setMaxAge(TWO_WEEKS_S);
		cookie.setPath(getCookiePath(request));
		cookie.setSecure(request.isSecure());
		cookie.setHttpOnly(true);
		return cookie;
	}
	
	// Mostly copied from TokenBasedRememberMeServices
	private String getCookiePath(HttpServletRequest request) {
		String contextPath = request.getContextPath();
		return (contextPath.length() > 0) ? contextPath : "/";
	}

	// private override
	private boolean isInstanceOfUserDetails(Authentication authentication) {
		return authentication.getPrincipal() instanceof UserDetails;
	}

	// Mostly copied from TokenBasedRememberMeServices
	public String getRememberMeCookieData(String username, String password) {
		long expiryTime = System.currentTimeMillis();
		// SEC-949
		expiryTime += 1000L * TWO_WEEKS_S;
		String signatureValue = makeTokenSignature(expiryTime, username, password);
		String cookieValue = encodeCookie(new String[] { username, Long.toString(expiryTime), signatureValue });
		return cookieValue;
	}
	
	public String getRememberMeCookieData(String username) {
		return getRememberMeCookieData(username, null);
	}
}
