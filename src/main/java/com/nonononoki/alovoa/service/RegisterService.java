package com.nonononoki.alovoa.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.util.Date;

import javax.mail.MessagingException;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.entity.Captcha;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.UserRegisterToken;
import com.nonononoki.alovoa.model.RegisterDto;
import com.nonononoki.alovoa.repo.CaptchaRepository;
import com.nonononoki.alovoa.repo.UserRegisterTokenRepository;
import com.nonononoki.alovoa.repo.UserRepository;

@Service
public class RegisterService {
	
	private final String TEMP_EMAIL_FILE_NAME = "temp-mail.txt";
	
	@Value("${app.token.length}")
	private int tokenLength;
	
	@Value("${app.age.min}")
	private int minAge;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@Autowired
	private MailService mailService;
	
	@Autowired
	private UserRepository userRepo;
	
	@Autowired
	private CaptchaRepository captchaRepo;
	
	@Autowired
	private UserRegisterTokenRepository registerTokenRepo;

	public boolean register(RegisterDto dto) throws MessagingException {
		
		//check minimum age
		LocalDate now = LocalDate.now();
		Period period = Period.between(now, new java.sql.Date(dto.getDateOfBirth().getTime()).toLocalDate());
		if(period.getYears() < (-1 * minAge)) {
			return false;
		}
		
		//check valid captcha
		Captcha captcha = captchaRepo.getOne(dto.getCaptchaId());
		if(captcha == null) {
			return false;
		}
		if(!captcha.getText().toLowerCase().equals(dto.getCaptchaText().toLowerCase())) {
			
			captchaRepo.delete(captcha);
			return false;
		}
		
		//check if email is already in use
		User user = new User();
		user.setEmail(dto.getEmail().toLowerCase());
		user.setDateOfBirth(dto.getDateOfBirth());
		user.setPassword(passwordEncoder.encode(dto.getPassword()));
		user.setFirstName(dto.getFirstName());
		
		//check if email is in spam mail list
		try {
			if(Tools.isTextContainingLineFromFile(Tools.getFileFromResources(TEMP_EMAIL_FILE_NAME), user.getEmail())) {
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		createUserToken(user);
		
		return true;
	}
	
	public void createUserToken(User user) throws MessagingException {
		UserRegisterToken token = generateToken(user);
		user.setRegisterToken(token);
		user = userRepo.save(user);
		mailService.sendRegistrationMail(user, token);
	}
	
	public UserRegisterToken generateToken(User user) {
		UserRegisterToken token = new UserRegisterToken();
		token.setContent(RandomStringUtils.randomAlphanumeric(tokenLength));
		token.setCreationDate(new Date());
		token.setUser(user);
		return registerTokenRepo.saveAndFlush(token);
	}
	
	public boolean registerConfirm(String tokenString) {
		UserRegisterToken token = registerTokenRepo.findByContent(tokenString);
		
		if(token == null) {
			return false;
		}
		
		User user = token.getUser();
		
		if(user == null) {
			return false;
		}
		
		if(user.isConfirmed()) {
			return false;
		}
		
		user.setConfirmed(true);
		userRepo.saveAndFlush(user);
		registerTokenRepo.delete(token);
		return true;
	}
}
