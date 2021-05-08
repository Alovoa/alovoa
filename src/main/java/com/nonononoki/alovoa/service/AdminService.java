package com.nonononoki.alovoa.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.Contact;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserReport;
import com.nonononoki.alovoa.model.MailDto;
import com.nonononoki.alovoa.model.UserDeleteParams;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.repo.ContactRepository;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.UserBlockRepository;
import com.nonononoki.alovoa.repo.UserHideRepository;
import com.nonononoki.alovoa.repo.UserLikeRepository;
import com.nonononoki.alovoa.repo.UserNotificationRepository;
import com.nonononoki.alovoa.repo.UserReportRepository;
import com.nonononoki.alovoa.repo.UserRepository;

@Service
public class AdminService {

	@Autowired
	private AuthService authService;
	
	@Autowired
	private MailService mailService;

	@Autowired
	private UserRepository userRepo;
	
	@Autowired
	private UserReportRepository userReportRepository;
	
	@Autowired
	private ContactRepository contactRepository;

	@Autowired
	private UserLikeRepository userLikeRepo;

	@Autowired
	private UserHideRepository userHideRepo;

	@Autowired
	private UserBlockRepository userBlockRepo;

	@Autowired
	private UserReportRepository userReportRepo;
	
	@Autowired
	private UserNotificationRepository userNotificationRepo;

	@Autowired
	private ConversationRepository conversationRepo;

	@Autowired
	private TextEncryptorConverter textEncryptor;
	
	
	public void hideContact(long id) throws Exception {	
		
		checkRights();
		
		Contact contact = contactRepository.findById(id).orElse(null);
		contact.setHidden(true);
		contactRepository.saveAndFlush(contact);
	}	
	
	public void sendMailSingle(MailDto dto) throws Exception {
		
		checkRights();
		
		mailService.sendAdminMail(dto.getEmail(), dto.getSubject(), dto.getBody());
	}
	
	public void sendMailAll(MailDto dto) throws Exception {		
		
		checkRights();
		
		List<User> users = userRepo.findByDisabledFalseAndAdminFalseAndConfirmedTrue();	
		mailService.sendAdminMailAll(dto.getSubject(), dto.getBody(), users);
	}

	public void deleteReport(long id) throws Exception {
		
		checkRights();
		
		UserReport report = userReportRepository.findById(id).get();
		User u = report.getUserFrom();
		u.getReported().remove(report);
		userRepo.saveAndFlush(u);
	}

	public void banUser(String id) throws NumberFormatException, Exception {
		
		checkRights();
		 
		User user = userRepo.findById(UserDto.decodeId(id, textEncryptor)).get();
		
		if(user == null) {
			throw new Exception("user_not_found");
		}
		
		UserDeleteParams userDeleteParam = UserDeleteParams.builder()
				.conversationRepo(conversationRepo)
				.userBlockRepo(userBlockRepo)
				.userHideRepo(userHideRepo)
				.userLikeRepo(userLikeRepo)
				.userNotificationRepo(userNotificationRepo)
				.userRepo(userRepo)
				.userReportRepo(userReportRepo)
				.build();
		
		UserService.removeUserLinkedLists(user, userDeleteParam);
		
		user.setAudio(null);
		user.setDates(null);
		user.setDeleteToken(null);
		user.setDescription(null);
		user.setLanguage(null);
		user.setAccentColor(null);
		user.setUiDesign(null);
		user.setDisabled(true);
		user.getDonations().clear();
		user.setFirstName(null);
		user.setGender(null);
		user.getImages().clear();
		user.setIntention(null);
		user.getInterests().clear();
		user.setLocationLatitude(null);
		user.setLocationLongitude(null);
		user.setPassword(null);
		user.setPasswordToken(null);
		user.setPreferedGenders(null);
		user.setPreferedMaxAge(0);
		user.setPreferedMinAge(0);
		user.setRegisterToken(null);
		user.setTotalDonations(0);
		user.setNumberProfileViews(0);
		user.setNumberSearches(0);
		user.getWebPush().clear();

		userRepo.saveAndFlush(user);
	}
	
	private void checkRights() throws Exception {
		if (!authService.getCurrentUser().isAdmin()) {
			throw new Exception("not_admin");
		}
	}


}
