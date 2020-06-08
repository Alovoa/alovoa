package com.nonononoki.alovoa.service;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.entity.Contact;
import com.nonononoki.alovoa.model.ContactDto;
import com.nonononoki.alovoa.repo.ContactRepository;

@Service
public class ImprintService {
	
	@Autowired
	private CaptchaService captchaService;
	
	@Autowired
	private PublicService publicService;
	
	@Autowired
	private ContactRepository contactRepo;
	
	public void contact(ContactDto dto) throws Exception {
		boolean isValid = captchaService.isValid(dto.getCaptchaId(), dto.getCaptchaText());
		if (!isValid) {
			throw new Exception(publicService.text("backend.error.captcha.invalid"));
		}
		
		Contact c = new Contact();
		c.setDate(new Date());
		c.setEmail(dto.getEmail());
		c.setMessage(dto.getMessage());
		contactRepo.save(c);
	}
}
