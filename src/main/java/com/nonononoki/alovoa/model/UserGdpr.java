package com.nonononoki.alovoa.model;

import java.util.List;
import java.util.Set;

import javax.persistence.Convert;

import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.Gender;
import com.nonononoki.alovoa.entity.user.Message;
import com.nonononoki.alovoa.entity.user.UserDates;
import com.nonononoki.alovoa.entity.user.UserDonation;
import com.nonononoki.alovoa.entity.user.UserImage;
import com.nonononoki.alovoa.entity.user.UserIntention;
import com.nonononoki.alovoa.entity.user.UserInterest;
import com.nonononoki.alovoa.entity.user.UserWebPush;

import lombok.Data;

@Data
public class UserGdpr {
	 
	private boolean confirmed;
	 
	private boolean disabled;

	private int preferedMinAge;

	private int preferedMaxAge;
	
	private Double locationLatitude;
	
	private Double locationLongitude;
	
	private double totalDonations;
	
	@Convert(converter = TextEncryptorConverter.class)
	private String email;

	private String firstName;

	private String description;

	private String profilePicture;
	
	private String audio;
	
	/*
	 * Custom classes
	 */

	private Gender gender;
	
	private UserIntention intention;
	
	private List<UserInterest> interests;

	private Set<Gender> preferedGenders;

	private List<UserImage> images;
	 
	private List<UserDonation> donations;
	 
//	private List<UserLike> likes;
	 
	private List<Message> messageSent;
//	 
//	private List<UserNotification> notifications;
//	 
//	private List<UserHide> hiddenUsers;
//	
//	private List<UserBlock> blockedUsers;
//	
//	private List<UserReport> reported;
	
	private List<UserWebPush> webPush;

	private UserDates dates;
	
	public static UserGdpr userToUserGdpr(User user) {
		UserGdpr u = new UserGdpr();
		u.setConfirmed(user.isConfirmed());
		u.setDisabled(user.isDisabled());
		u.setPreferedMinAge(user.getPreferedMinAge());
		u.setPreferedMaxAge(user.getPreferedMaxAge());
		u.setTotalDonations(user.getTotalDonations());
		
		u.setEmail(user.getEmail());
		u.setFirstName(user.getFirstName());
		u.setDescription(user.getDescription());
		u.setAudio(user.getAudio());
		
		u.setProfilePicture(user.getProfilePicture());
		
		u.setLocationLatitude(user.getLocationLatitude());
		u.setLocationLongitude(user.getLocationLongitude());
		
		u.setGender(user.getGender());
		u.setIntention(user.getIntention());
		u.setInterests(user.getInterests());
		u.setPreferedGenders(user.getPreferedGenders());
		u.setImages(user.getImages());
		u.setDonations(user.getDonations());
		u.setMessageSent(user.getMessageSent());
		u.setWebPush(user.getWebPush());
		u.setDates(user.getDates());

		return u;
	}
}
