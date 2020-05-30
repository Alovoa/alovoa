package com.nonononoki.alovoa.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.nonononoki.alovoa.entity.Gender;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.UserIntention;
import com.nonononoki.alovoa.repo.GenderRepository;
import com.nonononoki.alovoa.repo.UserIntentionRepository;
import com.nonononoki.alovoa.repo.UserRepository;

@Component
public class EventListenerConfig {

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private GenderRepository genderRepo;

	@Autowired
	private UserIntentionRepository userIntentionRepo;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Value("${app.admin.email}")
	private String adminEmail;

	@Value("${app.admin.key}")
	private String adminKey;

	@EventListener
	public void handleContextRefresh(ApplicationStartedEvent event) {
		setDefaultAdmin();
		setDefaultGenders();
		setDefaulIntentions();
	}
	
	public void setDefaultAdmin() {
		long noUsers = userRepo.count();
		if (noUsers == 0) {
			User user = new User();
			user.setAdmin(true);
			user.setEmail(adminEmail);
			String enc = passwordEncoder.encode(adminKey);
			user.setPassword(enc);
			userRepo.save(user);
		}
	}

	public void setDefaultGenders() {
		
		long noGenders = genderRepo.count();
		
		
		if (noGenders == 0) {
			Gender male = new Gender();
			male.setText("male");
			genderRepo.saveAndFlush(male);

			Gender female = new Gender();
			female.setText("female");
			genderRepo.saveAndFlush(female);

			Gender other = new Gender();
			other.setText("other");
			genderRepo.saveAndFlush(other);
		}
		
	}
	
	public void setDefaulIntentions() {
		long noIntentions = userIntentionRepo.count();
		if (noIntentions == 0) {
			UserIntention meet = new UserIntention();
			meet.setText("meet");
			userIntentionRepo.saveAndFlush(meet);

			UserIntention date = new UserIntention();
			date.setText("date");
			userIntentionRepo.saveAndFlush(date);

			UserIntention sex = new UserIntention();
			sex.setText("sex");
			userIntentionRepo.saveAndFlush(sex);
		}
	}

}
