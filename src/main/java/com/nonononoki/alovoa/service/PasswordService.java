package com.nonononoki.alovoa.service;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import javax.mail.MessagingException;
import javax.servlet.http.HttpSession;

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

	public UserPasswordToken resetPasword(PasswordResetDto dto)
			throws AlovoaException, NoSuchAlgorithmException, MessagingException, IOException {

		User u = authService.getCurrentUser();

		if (u == null) {
			if (!captchaService.isValid(dto.getCaptchaId(), dto.getCaptchaText())) {
				throw new AlovoaException("captcha_invalid");
			}
			u = userRepo.findByEmail(Tools.cleanEmail(dto.getEmail()));

			if (u == null) {
				throw new DisabledException("user_not_found");
			}

			if (u.isDisabled()) {
				throw new DisabledException("user_disabled");
			}
		}

		UserPasswordToken token = new UserPasswordToken();
		token.setContent(RandomStringUtils.randomAlphanumeric(tokenLength));
		token.setDate(new Date());
		token.setUser(u);
		u.setPasswordToken(token);
		u = userRepo.saveAndFlush(u);

		mailService.sendPasswordResetMail(u);
		
		SecurityContextHolder.clearContext();

		return u.getPasswordToken();
	}

	public void changePasword(PasswordChangeDto dto, HttpSession session) throws AlovoaException {
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
