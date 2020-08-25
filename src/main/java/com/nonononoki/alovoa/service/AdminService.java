package com.nonononoki.alovoa.service;

import java.util.ArrayList;
import java.util.List;

import javax.mail.MessagingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.Contact;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.MailDto;
import com.nonononoki.alovoa.repo.ContactRepository;
import com.nonononoki.alovoa.repo.UserReportRepository;
import com.nonononoki.alovoa.repo.UserRepository;

@Service
public class AdminService {

	@Autowired
	private MailService mailService;

	@Autowired
	private UserRepository userRepo;
	
	@Autowired
	private UserReportRepository userReportRepository;
	
	@Autowired
	private ContactRepository contactRepository;

	@Autowired
	private TextEncryptorConverter textEncryptor;
	
	
	public void hideContact(long id) throws Exception {	
		Contact contact = contactRepository.findById(id).orElse(null);
		contact.setHidden(true);
		contactRepository.saveAndFlush(contact);
	}	
	
	public void sendMailSingle(MailDto dto) throws MessagingException {
		mailService.sendAdminMail(dto.getEmail(), dto.getSubject(), dto.getBody());
	}
	
	public void sendMailAll(MailDto dto) throws MessagingException {		
		List<User> users = userRepo.findAll();	
		for(User u : users) {
			mailService.sendAdminMail(u.getEmail(), dto.getSubject(), dto.getBody());
		}		
	}

	public void deleteReport(long id) {
		userReportRepository.deleteById(id);
	}

	public void banUser(String id) throws NumberFormatException, Exception {
		User user = userRepo.findById(Long.parseLong(textEncryptor.decode(id))).orElse(null);
		
		if(user == null) {
			throw new Exception("");
		}
		
		user.getBlockedByUsers().clear();
		user.getConversationsBy().clear();
		user.getHiddenByUsers().clear();
		user.getReportedByUsers().clear();
		user.getBlockedUsers().clear();
		user.getConversations().clear();
		user.getHiddenUsers().clear();
		user.getReported().clear();
		user.getImages().clear();
		user.getLikedBy().clear();
		user.getLikes().clear();
		user.getMessageReceived().clear();
		user.getMessageSent().clear();
		user.getNotifications().clear();
		user.getNotificationsFrom().clear();
		user.getWebPush().clear();
		
		user.setDates(null);
		user.setFirstName(null);
		user.setLastLocation(null);
		user.setPassword(null);
		user.setPasswordToken(null);
		user.setProfilePicture(null);
		user.setDisabled(true);

		user = userRepo.saveAndFlush(user);
		return;
		
	}


}
