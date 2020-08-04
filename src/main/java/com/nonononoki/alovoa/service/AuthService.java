package com.nonononoki.alovoa.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.repo.UserRepository;

@Service
public class AuthService {

	@Autowired
	private UserRepository userRepo;

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

	public User getCurrentUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String email;
		if (auth instanceof OAuth2AuthenticationToken) {
			auth = (OAuth2AuthenticationToken) auth;
			DefaultOAuth2User principal = (DefaultOAuth2User) auth.getPrincipal();
			email = principal.getAttribute("email");
		} else {
			email = (String) auth.getPrincipal();
		}
		
		return userRepo.findByEmail(email);
	}
}
