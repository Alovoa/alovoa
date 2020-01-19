package com.nonononoki.alovoa.model;

import java.util.Date;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nonononoki.alovoa.entity.Gender;
import com.nonononoki.alovoa.entity.UserImage;
import com.nonononoki.alovoa.entity.UserIntention;

import lombok.Data;

@Data
public class UserDto {
	
	@JsonIgnore
	private long id;
	
	private String firstName;
	private int age;
	private Gender gender;
	private Set<Gender> preferedGenders;
	private UserIntention intention;
	
	private String profilePicture;
	private List<UserImage> images;
	
	private String description;
	
	private double distanceToUser;
	private double totalDonations;
	
	private Date activeDate;
	
	private long numberOfBlocks;
	private long numberOfReports;
}
