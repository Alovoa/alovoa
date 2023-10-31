package com.nonononoki.alovoa.component;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.config.SecurityConfig;
import com.nonononoki.alovoa.entity.Captcha;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.AuthToken;
import com.nonononoki.alovoa.repo.CaptchaRepository;
import com.nonononoki.alovoa.repo.UserRepository;

@Component
public class AuthProvider implements AuthenticationProvider {

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private CaptchaRepository captchaRepo;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Value("${app.captcha.login.enabled}")
	private String captchaEnabled;

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(AuthProvider.class);

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {

		AuthToken a = (AuthToken) authentication;
		String email = Tools.cleanEmail(a.getUsername());
		String password = a.getPassword();
		long captchaId = a.getCaptchaId();
		String captchaText = a.getCaptchaText();

		if (Boolean.parseBoolean(captchaEnabled)) {
			logger.debug("Captcha enabled, so check captcha");
			Captcha c = captchaRepo.findById(captchaId).orElse(null);
			if (c == null) {
				throw new BadCredentialsException("");
			}

			captchaRepo.delete(c);

			if (!c.getText().equalsIgnoreCase(captchaText)) {
				throw new BadCredentialsException("");
			}
		} else {
			logger.debug("Captcha disabled, so we do not care about it");
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
			throw new InsufficientAuthenticationException("");
		}
		
		List<GrantedAuthority> authorities = new ArrayList<>();
		String role;
		if (user.isAdmin()) {
			role = SecurityConfig.getRoleAdmin();
		} else {
			role = SecurityConfig.getRoleUser();
		}
		authorities.add(new SimpleGrantedAuthority(role));

		return new UsernamePasswordAuthenticationToken(email, password, authorities);
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return authentication.equals(UsernamePasswordAuthenticationToken.class)
				|| authentication.equals(AuthToken.class);
	}
}
