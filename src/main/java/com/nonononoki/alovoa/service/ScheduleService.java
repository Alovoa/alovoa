package com.nonononoki.alovoa.service;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.repo.CaptchaRepository;
import com.nonononoki.alovoa.repo.UserHideRepository;

@Service
@EnableScheduling
public class ScheduleService {
	
	@Autowired
	private CaptchaRepository captchaRepo;
	
	@Autowired
	private UserHideRepository userHideRepo;
	
	@Value("${app.schedule.delay.captcha}")
	private long captchaDelay;
	
	@Value("${app.schedule.delay.hide}")
	private long hideDelay;
	
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
	
}
