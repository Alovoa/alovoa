package com.nonononoki.alovoa.service;

import com.nonononoki.alovoa.component.ExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.repo.UserRepository;

@Service
public class AuthService {

	@Autowired
	private UserRepository userRepo;

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

	public User getCurrentUser(boolean throwExceptionWhenNull) throws AlovoaException {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String email;
		if (auth instanceof OAuth2AuthenticationToken) {
			DefaultOAuth2User principal = (DefaultOAuth2User) auth.getPrincipal();
			email = principal.getAttribute("email");
		} else {
			if (auth == null) {
				return null;
			} else if (auth.getPrincipal() instanceof User) {
				email = ((User) auth.getPrincipal()).getEmail();
			} else {
				email = (String) auth.getPrincipal();
			}
		}

		User user = userRepo.findByEmail(Tools.cleanEmail(email));
		if (user != null && user.isDisabled()) {
			throw new AlovoaException(ExceptionHandler.USER_NOT_FOUND);
		} else if (user == null && throwExceptionWhenNull) {
			//try again
            user = userRepo.findByEmail(Tools.cleanEmail(email));
			if (user == null) {
				throw new AlovoaException(ExceptionHandler.USER_NOT_FOUND);
			}
		}

		return user;
	}
	
	public User getCurrentUser() throws AlovoaException {
		return getCurrentUser(false);
	}

	public String getOauth2Email() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String email = null;
		if (auth instanceof OAuth2AuthenticationToken) {
			DefaultOAuth2User principal = (DefaultOAuth2User) auth.getPrincipal();
			email = principal.getAttribute("email");
		}
		return email;
	}
}
