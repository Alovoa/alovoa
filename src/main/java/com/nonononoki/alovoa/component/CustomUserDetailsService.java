package com.nonononoki.alovoa.component;

import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.repo.UserRepository;

import java.util.UUID;

@Service
@AllArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private UserRepository userRepo;

	@Override
	public UserDetails loadUserByUsername(String username) {

        User user = null;
        try{
            UUID uuid = UUID.fromString(username);
            user = userRepo.findByUuid(uuid);
        } catch (IllegalArgumentException exception){
            user = userRepo.findByEmail(username);
        }
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
		return user;
	}
}