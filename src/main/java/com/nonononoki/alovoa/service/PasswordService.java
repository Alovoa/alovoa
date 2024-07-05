package com.nonononoki.alovoa.service;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;

import com.nonononoki.alovoa.component.ExceptionHandler;
import jakarta.mail.MessagingException;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserPasswordToken;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.PasswordChangeDto;
import com.nonononoki.alovoa.model.PasswordResetDto;
import com.nonononoki.alovoa.repo.UserPasswordTokenRepository;
import com.nonononoki.alovoa.repo.UserRepository;

@Service
public class PasswordService {

	@Autowired
	private UserPasswordTokenRepository userPasswordTokenRepo;

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private CaptchaService captchaService;

	@Autowired
	private AuthService authService;

	@Autowired
	private MailService mailService;
	
	@Value("${app.password-token.length}")
	private int tokenLength;
	
	@Value("${app.user.password-reset.duration.valid}")
	private int userPasswordResetDuration;

	public UserPasswordToken resetPassword(PasswordResetDto dto)
			throws AlovoaException, NoSuchAlgorithmException, MessagingException, IOException {

		if (!captchaService.isValid(dto.getCaptchaId(), dto.getCaptchaText())) {
			throw new AlovoaException("captcha_invalid");
		}
		User u = userRepo.findByEmail(Tools.cleanEmail(dto.getEmail()));

		if (u == null) {
			throw new AlovoaException(ExceptionHandler.USER_NOT_FOUND);
		}

		if (u.isAdmin()) {
			throw new AlovoaException("user_is_admin");
		}

		//user has social login, do not assign new password!
		if (u.getPassword() == null) {
			throw new AlovoaException("user_has_social_login");
		}

		if (u.isDisabled()) {
			throw new AlovoaException("user_disabled");
		}

		UserPasswordToken token = new UserPasswordToken();
		token.setContent(RandomStringUtils.random(tokenLength, 0, 0, true, true, null, new SecureRandom()));
		token.setDate(new Date());
		token.setUser(u);
		u.setPasswordToken(token);
		u = userRepo.saveAndFlush(u);

		mailService.sendPasswordResetMail(u);

		SecurityContextHolder.clearContext();

		return u.getPasswordToken();
	}

	public void changePassword(PasswordChangeDto dto) throws AlovoaException {
		UserPasswordToken token = userPasswordTokenRepo.findByContent(dto.getToken());
		if (token == null) {
			throw new AlovoaException("token_not_found");
		}
		if (!token.getContent().equals(dto.getToken())) {
			throw new AlovoaException("token_wrong_content");
		}
		User user = token.getUser();
		
		if (!user.getEmail().equals(Tools.cleanEmail(dto.getEmail()))) {
			throw new AlovoaException("wrong_email");
		}
		if (user.isAdmin()) {
			throw new AlovoaException("user_is_admin");
		}
		
		long ms = new Date().getTime();
		if (ms - user.getPasswordToken().getDate().getTime() > userPasswordResetDuration) {
			throw new AlovoaException("deletion_not_valid");
		}
		
		user.setPassword(passwordEncoder.encode(dto.getPassword()));
		user.setPasswordToken(null);

		if (!user.isConfirmed()) {
			user.setConfirmed(true);
			user.setRegisterToken(null);
		}

		SecurityContextHolder.clearContext();

		userRepo.saveAndFlush(user);
	}
}
