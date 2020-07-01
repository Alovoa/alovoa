package com.nonononoki.alovoa.service;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.entity.Gender;
import com.nonononoki.alovoa.entity.Location;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.UserIntention;
import com.nonononoki.alovoa.repo.GenderRepository;
import com.nonononoki.alovoa.repo.UserIntentionRepository;
import com.nonononoki.alovoa.repo.UserRepository;

@Service
public class AdminService {

	@Autowired
	private AuthService authService;
	
	@Autowired
	private NotificationService notificationService;

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private GenderRepository genderRepo;

	@Autowired
	private UserIntentionRepository intentRepo;

	

	public void testNotificationLike() throws Exception {
		User user = authService.getCurrentUser();
		notificationService.newLike(user);
	}
	
	public void testNotificationMatch() throws Exception {
		User user = authService.getCurrentUser();
		notificationService.newMatch(user);
	}
	
	public void testNotificationMessage() throws Exception {
		User user = authService.getCurrentUser();
		notificationService.newMessage(user);
	}

}
