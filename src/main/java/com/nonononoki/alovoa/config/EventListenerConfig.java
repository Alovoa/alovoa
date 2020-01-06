package com.nonononoki.alovoa.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.repo.UserRepository;

@Component
public class EventListenerConfig {
	
	@Autowired
	private UserRepository userRepo;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@Value("${app.admin.email}")
	private String adminEmail;
	
	@Value("${app.admin.key}")
	private String adminKey;
	
    @EventListener
    public void handleContextRefresh(ApplicationStartedEvent event) {
    	long noUsers = userRepo.count();
    	if(noUsers == 0) {
    		User user = new User();
    		user.setAdmin(true);
    		user.setEmail(adminEmail);
    		String enc = passwordEncoder.encode(adminKey);
    		user.setPassword(enc);
    		userRepo.save(user);
    	}
    }
	
}
