package com.nonononoki.alovoa.component;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.util.StringUtils;

public class CustomTokenBasedRememberMeServices extends TokenBasedRememberMeServices {

	public CustomTokenBasedRememberMeServices(String key, UserDetailsService userDetailsService) {
		super(key, userDetailsService);
	}

	protected String retrieveUserName(Authentication authentication) {
		if (isInstanceOfUserDetails(authentication)) {
			return ((UserDetails) authentication.getPrincipal()).getUsername();
		}
		return (String) ((DefaultOAuth2User) authentication.getPrincipal()).getAttributes().get("email");
	}
	
	//Mostly copied from TokenBasedRememberMeServices
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
			} catch(UsernameNotFoundException e) {
				password = "";
			}
		}
		int tokenLifetime = calculateLoginLifetime(request, successfulAuthentication);
		long expiryTime = System.currentTimeMillis();
		// SEC-949
		expiryTime += 1000L * ((tokenLifetime < 0) ? TWO_WEEKS_S : tokenLifetime);
		String signatureValue = makeTokenSignature(expiryTime, username, password);
		setCookie(new String[] { username, Long.toString(expiryTime), signatureValue }, tokenLifetime, request,
				response);
		if (this.logger.isDebugEnabled()) {
			this.logger.debug(
					"Added remember-me cookie for user '" + username + "', expiry: '" + new Date(expiryTime) + "'");
		}
	}
	
	//private override
	private boolean isInstanceOfUserDetails(Authentication authentication) {
		return authentication.getPrincipal() instanceof UserDetails;
	}
	
}
