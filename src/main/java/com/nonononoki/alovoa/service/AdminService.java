package com.nonononoki.alovoa.service;

import java.util.List;

import javax.mail.MessagingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.MailDto;
import com.nonononoki.alovoa.repo.GenderRepository;
import com.nonononoki.alovoa.repo.UserIntentionRepository;
import com.nonononoki.alovoa.repo.UserRepository;

@Service
public class AdminService {

	@Autowired
	private AuthService authService;

	@Autowired
	private MailService mailService;

	@Autowired
	private UserRepository userRepo;
	
	
	public void sendMailSingle(MailDto dto) throws MessagingException {
		mailService.sendAdminMail(dto.getRecipient(), dto.getSubject(), dto.getBody());
	}
	
	public void sendMailAll(MailDto dto) throws MessagingException {		
		List<User> users = userRepo.findAll();	
		for(User u : users) {
			mailService.sendAdminMail(u.getEmail(), dto.getSubject(), dto.getBody());
		}		
	}


}
