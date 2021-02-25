package com.nonononoki.alovoa.service;

import java.util.Date;
import java.util.List;

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
			throw new Exception("");
		}
		User u = userRepo.findByEmail(dto.getEmail().toLowerCase());
		
		List<User> users = userRepo.findAll();
		
		if(u.isDisabled()) {
			throw new DisabledException("");
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
			throw new Exception("");
		}
		if (!token.getContent().equals(dto.getToken())) {
			throw new Exception("");
		}
		User user = token.getUser();
		if(!user.getEmail().equals(dto.getEmail().toLowerCase())) {
			throw new Exception("");
		}
		user.setPassword(passwordEncoder.encode(dto.getPassword()));
		user.setPasswordToken(null);
		userRepo.saveAndFlush(user);
	}
}
