package com.nonononoki.alovoa.model;

import java.util.Set;

import com.nonononoki.alovoa.entity.user.Gender;
import com.nonononoki.alovoa.entity.user.UserIntention;
import com.nonononoki.alovoa.entity.user.UserProfilePicture;

import lombok.Data;

@Data
public class UserOnboardingDto {
	
	private UserIntention intention;
	
	private Set<Gender> preferedGenders;
	
	private UserProfilePicture profilePicture;

	private String description;
	
}
