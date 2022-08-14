package com.nonononoki.alovoa.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserHide;
import com.nonononoki.alovoa.repo.CaptchaRepository;
import com.nonononoki.alovoa.repo.ContactRepository;
import com.nonononoki.alovoa.repo.UserHideRepository;
import com.nonononoki.alovoa.repo.UserRepository;

@Transactional
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

	@Value("${app.schedule.enabled}")
	private boolean enableSchedules;

	@Value("${app.schedule.delay.captcha}")
	private long captchaDelay;

	@Value("${app.schedule.delay.hide}")
	private long hideDelay;

	@Value("${app.schedule.delay.contact}")
	private long contactDelay;

	private static final int HIDE_MAX = 20;

	@Scheduled(fixedDelayString = "${app.schedule.short}")
	public void scheduleShort() {
		if (enableSchedules) {
			Date date = new Date();
			cleanCaptcha(date);
			cleanUserHide(date);
		}
	}

	@Scheduled(fixedDelayString = "${app.schedule.long}")
	public void scheduleLong() {
		if (enableSchedules) {
			Date date = new Date();
			cleanContact(date);
		}
	}

	public void cleanCaptcha(Date date) {
		long ms = date.getTime();
		ms -= captchaDelay;
		Date d = new Date(ms);
		captchaRepo.deleteByDateBefore(d);
		captchaRepo.flush();
	}

	public void cleanContact(Date date) {
		long ms = date.getTime();
		ms -= contactDelay;
		Date d = new Date(ms);
		contactRepo.deleteByDateBefore(d);
		contactRepo.flush();
	}

	public void cleanUserHide(Date date) {
		long ms = date.getTime();
		ms -= hideDelay;
		Date d = new Date(ms);

		List<UserHide> hides = userHideRepo.findByByDateBefore(d,
				PageRequest.of(0, HIDE_MAX, Sort.by(Sort.Direction.ASC, "date")));
		List<UserHide> emptyHides = new ArrayList<>();
		List<User> users = new ArrayList<>();
		for (UserHide hide : hides) {
			User u = hide.getUserFrom();
			if(u == null || u.getHiddenByUsers() == null) {
				emptyHides.add(hide);
			} else {
				u.getHiddenUsers().remove(hide);
				users.add(u);
			} 
		}
		userRepo.saveAll(users);
		userHideRepo.deleteAll(emptyHides);
		userRepo.flush();
		userHideRepo.flush();
	}

}
