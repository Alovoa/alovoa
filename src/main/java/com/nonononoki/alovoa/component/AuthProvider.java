package com.nonononoki.alovoa.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.nonononoki.alovoa.entity.Captcha;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.AuthToken;
import com.nonononoki.alovoa.repo.CaptchaRepository;
import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.service.RegisterService;

@Component
public class AuthProvider implements AuthenticationProvider {

	@Autowired
	private RegisterService registerService;

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private CaptchaRepository captchaRepo;

	@Autowired
	private PasswordEncoder passwordEncoder;
	
	private static final Logger logger = LoggerFactory.getLogger(AuthProvider.class);

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {

		AuthToken a = (AuthToken) authentication;
		String email = a.getUsername();
		String password = a.getPassword();
		long captchaId = a.getCaptchaId();
		String captchaText = a.getCaptchaText();

		Captcha c = captchaRepo.findById(captchaId).orElse(null);
		if (c == null) {
			throw new BadCredentialsException("");
		}

		captchaRepo.delete(c);

		if (!c.getText().toLowerCase().equals(captchaText.toLowerCase())) {
			throw new BadCredentialsException("");
		}

		User user = userRepo.findByEmail(email);

		if (user == null) {
			throw new BadCredentialsException("");
		}
		if (user.isDisabled()) {
			throw new DisabledException("");
		}

		if (password.isEmpty()) {
			throw new BadCredentialsException("");
		}

		if (!passwordEncoder.matches(password, user.getPassword())) {
			throw new BadCredentialsException("");
		}
		if (!user.isConfirmed() && !user.isAdmin()) {

			try {
				registerService.createUserToken(user);
			} catch (Exception e) {
				logger.error(e.getMessage());
			}

			throw new InsufficientAuthenticationException("");
		}

		return new UsernamePasswordAuthenticationToken(email, password, null);
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return authentication.equals(UsernamePasswordAuthenticationToken.class)
				|| authentication.equals(AuthToken.class);
	}
}
