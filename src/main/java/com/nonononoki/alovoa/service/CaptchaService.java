package com.nonononoki.alovoa.service;

import java.util.Base64;
import java.util.Date;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.github.cage.Cage;
import com.github.cage.GCage;
import com.nonononoki.alovoa.entity.Captcha;
import com.nonononoki.alovoa.repo.CaptchaRepository;

@Service
public class CaptchaService {
	
	@Autowired
	private CaptchaRepository captchaRepo;
	
	@Value("${app.captcha.length}")
	private int captchaLength;

	public Captcha generate() {
		Cage cage = new GCage();
		String rand = RandomStringUtils.randomAlphanumeric(captchaLength);
		byte[] ba = cage.draw(rand);
		String encoded = Base64.getEncoder().encodeToString(ba);
		Captcha captcha = new Captcha();
		captcha.setDate(new Date());
		captcha.setImage(encoded);
		captcha.setText(rand);
		captcha = captchaRepo.saveAndFlush(captcha);
		return captcha;
	}
}
