package com.nonononoki.alovoa.model;

import java.util.List;
import java.util.Set;

import lombok.Data;

@Data
public class ProfileOnboardingDto {
	
	private Long intention;
	
	private List<Long> preferredGenders;
	
	private String profilePicture;

	private String description;
	
	private Set<String> interests;
	
}
