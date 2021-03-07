package com.nonononoki.alovoa.service;

import java.util.Date;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserPasswordToken;
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
	private MailService mailService;

	@Value("${app.password-token.length}")
	private int tokenLength;

	public UserPasswordToken resetPasword(PasswordResetDto dto) throws Exception {
		if (!captchaService.isValid(dto.getCaptchaId(), dto.getCaptchaText())) {
			throw new Exception("captcha_invalid");
		}
		User u = userRepo.findByEmail(dto.getEmail().toLowerCase());
		
		if(u.isDisabled()) {
			throw new DisabledException("user_disabled");
		}
		
		UserPasswordToken token = new UserPasswordToken();
		token.setContent(RandomStringUtils.randomAlphanumeric(tokenLength));
		token.setDate(new Date());
		token.setUser(u);
		u.setPasswordToken(token);
		u = userRepo.saveAndFlush(u);

		mailService.sendPasswordResetMail(u, token);
		
		return u.getPasswordToken();
	}

	public void changePasword(PasswordChangeDto dto) throws Exception {
		UserPasswordToken token = userPasswordTokenRepo.findByContent(dto.getToken());
		if (token == null) {
			throw new Exception("token_not_found");
		}
		if (!token.getContent().equals(dto.getToken())) {
			throw new Exception("token_wrong_content");
		}
		User user = token.getUser();
		if(!user.getEmail().equals(dto.getEmail().toLowerCase())) {
			throw new Exception("wrong_email");
		}
		user.setPassword(passwordEncoder.encode(dto.getPassword()));
		user.setPasswordToken(null);
		userRepo.saveAndFlush(user);
	}
}
