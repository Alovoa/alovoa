package com.nonononoki.alovoa.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.entity.Captcha;
import com.nonononoki.alovoa.entity.Contact;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserDeleteToken;
import com.nonononoki.alovoa.entity.user.UserHide;
import com.nonononoki.alovoa.entity.user.UserPasswordToken;
import com.nonononoki.alovoa.repo.CaptchaRepository;
import com.nonononoki.alovoa.repo.ContactRepository;
import com.nonononoki.alovoa.repo.UserDeleteTokenRepository;
import com.nonononoki.alovoa.repo.UserHideRepository;
import com.nonononoki.alovoa.repo.UserPasswordTokenRepository;
import com.nonononoki.alovoa.repo.UserRepository;

@Service
public class ScheduleService {
	
	@Autowired
	private CaptchaRepository captchaRepo;
	
	@Autowired
	private ContactRepository contactRepo;
	
	@Autowired
	private UserHideRepository userHideRepo;
	
	@Autowired
	private UserRepository userRepo;
	
	@Autowired
	private UserPasswordTokenRepository passwordTokenRepository;
	
	@Autowired
	private UserDeleteTokenRepository userDeleteTokenRepository;
	
	@Value("${app.schedule.delay.captcha}")
	private long captchaDelay;
	
	@Value("${app.schedule.delay.hide}")
	private long hideDelay;
	
	@Value("${app.schedule.delay.password-reset}")
	private long passwordResetDelay;
	
	@Value("${app.schedule.delay.delete-account}")
	private long deleteAccountDelay;
	
	@Value("${app.schedule.delay.contact}")
	private long contactDelay;
	
	
	@Scheduled( fixedDelayString = "${app.schedule.short}")
	@ConditionalOnProperty(value = "app.schedule.enabled", matchIfMissing = true, havingValue = "true")
	public void scheduleShort() {
		Date date = new Date();
		cleanCaptcha(date);
		cleanUserPasswordToken(date);
	}
	
	@Scheduled( fixedDelayString = "${app.schedule.long}")
	@ConditionalOnProperty(value = "app.schedule.enabled", matchIfMissing = true, havingValue = "true")
	public void scheduleLong() {
		Date date = new Date();
		cleanUserHide(date);
		cleanUserDeleteToken(date);
	}
	
	public void cleanCaptcha(Date date) {
		long ms = date.getTime();
		ms -= captchaDelay;
		Date d = new Date(ms);
		
		List<Captcha> captchas = captchaRepo.findByDateBefore(d);
		captchaRepo.deleteAll(captchas);
	}

	public void cleanUserHide(Date date) {
		long ms = date.getTime();
		ms -= hideDelay;
		Date d = new Date(ms);
		
		List<UserHide> tokens = userHideRepo.findByDateBefore(d);
		List<User> users = new ArrayList<>();
		for(UserHide hide : tokens ) {
			User u = hide.getUserFrom();
			User u2 =hide.getUserTo();
			u.getHiddenUsers().remove(hide);
			u2.getHiddenByUsers().remove(hide);
			users.add(u);
			users.add(u2);
			userHideRepo.delete(hide);
		}
		userRepo.saveAll(users);
		userRepo.flush();
		userHideRepo.flush();
	}
	
	public void cleanUserPasswordToken(Date date) {
		long ms = date.getTime();
		ms -= passwordResetDelay;
		Date d = new Date(ms);
		
		List<UserPasswordToken> tokens = passwordTokenRepository.findByDateBefore(d);
		List<User> users = new ArrayList<>();
		for(UserPasswordToken token : tokens ) {
			User u = token.getUser();
			u.setPasswordToken(null);
			users.add(u);
		}
		userRepo.saveAll(users);
		userRepo.flush();
		passwordTokenRepository.flush();
	}
	
	public void cleanUserDeleteToken(Date date) {
		long ms = date.getTime();
		ms -= deleteAccountDelay;
		Date d = new Date(ms);
		
		List<UserDeleteToken> tokens = userDeleteTokenRepository.findByActiveDateBefore(d);
		List<User> users = new ArrayList<>();
		for(UserDeleteToken token : tokens ) {
			User u = token.getUser();
			u.setDeleteToken(null);
			users.add(u);
		}
		userRepo.saveAll(users);
		userRepo.flush();
		userDeleteTokenRepository.flush();
	}
	
}
