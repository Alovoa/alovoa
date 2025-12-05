package com.nonononoki.alovoa.service;

import com.nonononoki.alovoa.component.ExceptionHandler;
import com.nonononoki.alovoa.model.CustomOAuth2User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.repo.UserRepository;

import java.util.UUID;

@Service
public class AuthService {

	@Autowired
	private UserRepository userRepo;

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

	public synchronized User getCurrentUser(boolean throwExceptionWhenNull) throws AlovoaException {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = null;
		if (auth instanceof OAuth2AuthenticationToken) {
            OAuth2User principal = (OAuth2User) auth.getPrincipal();
            if(principal instanceof CustomOAuth2User) {
                user = userRepo.findByUuid(((CustomOAuth2User) principal).getUuid());
            } else {
                String email = principal.getAttribute("email");
                user = userRepo.findByEmail(Tools.cleanEmail(email));
            }
		} else {
			if (auth == null) {
				return null;
			} else if (auth.getPrincipal() instanceof User) {
				user = (User) auth.getPrincipal();
			} else {
				String username = (String) auth.getPrincipal();
                try{
                    UUID uuid = UUID.fromString(username);
                    user = userRepo.findByUuid(uuid);
                } catch (IllegalArgumentException exception){
                    user = userRepo.findByEmail(Tools.cleanEmail(username));
                }
			}
		}

		if (user == null) {
			throw new AlovoaException(ExceptionHandler.USER_NOT_FOUND);
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
            OAuth2User principal = (OAuth2User) auth.getPrincipal();
			email = principal.getAttribute("email");
		}
		return email;
	}
}
