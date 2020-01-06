package com.nonononoki.alovoa.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.model.AuthSession;

@Service
public class AuthService {
	
	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
	
	public AuthSession getCurrentSession() {
		Object obj = SecurityContextHolder.getContext().getAuthentication().getDetails();
        if (obj instanceof AuthSession) {
        	AuthSession session = (AuthSession) obj;
            return session;
        } else {
        	return null;
        }
	}
	
	public long getCurrentUserId() {
		AuthSession session = getCurrentSession();
		if(session != null) {
			return session.getId();
		} else {
			return -1;
		}
	}
	
	public String getCurrentUserEmail() {
		AuthSession session = getCurrentSession();
		if(session != null) {
			return session.getEmail();
		} else {
			return null;
		}
    }
}
