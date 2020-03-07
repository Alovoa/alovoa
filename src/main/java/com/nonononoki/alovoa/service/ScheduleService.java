package com.nonononoki.alovoa.service;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.repo.CaptchaRepository;
import com.nonononoki.alovoa.repo.UserHideRepository;
import com.nonononoki.alovoa.repo.UserPasswordTokenRepository;

@Service
@EnableScheduling
public class ScheduleService {
	
	@Autowired
	private CaptchaRepository captchaRepo;
	
	@Autowired
	private UserHideRepository userHideRepo;
	
	@Autowired
	private UserPasswordTokenRepository passwordTokenRepository;
	
	@Value("${app.schedule.delay.captcha}")
	private long captchaDelay;
	
	@Value("${app.schedule.delay.hide}")
	private long hideDelay;
	
	@Value("${app.schedule.delay.password-reset}")
	private long passwordResetDelay;
	
	@Scheduled( fixedDelayString = "${app.schedule.delay.captcha}")
	public void cleanCaptcha() {
		Date date = new Date();
		long ms = date.getTime();
		ms -= captchaDelay;
		date = new Date(ms);
		
		captchaRepo.deleteAll(captchaRepo.findByDateBefore(date));
	}
	
	@Scheduled( fixedDelayString = "${app.schedule.delay.hide}")
	public void cleanHide() {
		Date date = new Date();
		long ms = date.getTime();
		ms -= hideDelay;
		date = new Date(ms);
		
		userHideRepo.deleteAll(userHideRepo.findByDateBefore(date));
	}
	
	@Scheduled( fixedDelayString = "${app.schedule.delay.hide}")
	public void cleanPasswordToken() {
		Date date = new Date();
		long ms = date.getTime();
		ms -= passwordResetDelay;
		date = new Date(ms);
		
		passwordTokenRepository.deleteAll(passwordTokenRepository.findByDateBefore(date));
	}
	
}
