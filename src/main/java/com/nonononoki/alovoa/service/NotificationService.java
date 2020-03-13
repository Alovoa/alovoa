package com.nonononoki.alovoa.service;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.repo.UserRepository;

@Service
public class NotificationService {

	@Autowired
	private UserRepository userRepo;

	public void newLike(User user) {
		user.getDates().setNotificationDate(new Date());
		user = userRepo.saveAndFlush(user);
		
		//TODO
	}
	
	public void newMatch(User user) {
		user.getDates().setMessageDate(new Date());
		user = userRepo.saveAndFlush(user);
		
		//TODO
	}
	
	public void newMessage(User user) {
		user.getDates().setMessageDate(new Date());
		user = userRepo.saveAndFlush(user);
		
		//TODO
	}

}
