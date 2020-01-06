package com.nonononoki.alovoa.component;

import javax.mail.MessagingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.service.RegisterService;

@Component
public class AuthProvider implements AuthenticationProvider {
	
	@Autowired
	private RegisterService registerService;
	
	@Autowired
	private UserRepository userRepo;
	
	@Autowired
	private PasswordEncoder passwordEncoder;

	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
	    String email = null;
	    String password = null; 

	    try {
	    	email = authentication.getPrincipal().toString();
	    	password = authentication.getCredentials().toString();
	    } catch(Exception e) {
	    	throw new BadCredentialsException("");
	    }

	    User user = userRepo.findByEmail(email);
	    
	    if (user == null) {
	        throw new BadCredentialsException("");
	    }
	    if (user.isDisabled()) {
	        throw new DisabledException("");
	    }
	    if (!passwordEncoder.matches(password, user.getPassword())) {
	        throw new BadCredentialsException("");
	    } 
	    if(!user.isConfirmed() && !user.isAdmin()) {
	    	
	    	try {
				registerService.createUserToken(user);
			} catch (MessagingException e) {
				e.printStackTrace();
			}
	    	
	    	throw new InsufficientAuthenticationException("");
	    }
	    
	    return new UsernamePasswordAuthenticationToken(email, password, null);
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return authentication.equals(UsernamePasswordAuthenticationToken.class);
	}
}
