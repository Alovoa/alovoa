package com.nonononoki.alovoa.model;

import java.util.List;
import java.util.Set;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.Gender;
import com.nonononoki.alovoa.entity.user.Message;
import com.nonononoki.alovoa.entity.user.UserDates;
import com.nonononoki.alovoa.entity.user.UserDonation;
import com.nonononoki.alovoa.entity.user.UserImage;
import com.nonononoki.alovoa.entity.user.UserIntention;
import com.nonononoki.alovoa.entity.user.UserInterest;

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

	private String email;

	private String firstName;

	private String description;

	private String profilePicture;

	private String audio;

	private String language;

	private String accentColor;

	private String uiDesign;

	private int units;

	private Gender gender;

	private UserIntention intention;

	private List<UserInterest> interests;

	private Set<Gender> preferedGenders;

	private List<UserImage> images;

	private List<UserDonation> donations;

	private List<Message> messageSent;

	// private List<UserWebPush> webPush;

	private UserDates dates;

	long numberProfileViews;

	long numberSearches;
	
	private String zodiac;
	
	private boolean showZodiac;

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
		u.setLanguage(user.getLanguage());
		u.setAccentColor(user.getAccentColor());
		u.setUiDesign(user.getUiDesign());
		u.setUnits(user.getUnits());

		u.setLocationLatitude(user.getLocationLatitude());
		u.setLocationLongitude(user.getLocationLongitude());

		u.setGender(user.getGender());
		u.setIntention(user.getIntention());
		u.setInterests(user.getInterests());
		u.setPreferedGenders(user.getPreferedGenders());

		if (user.getAudio() != null)
			u.setAudio(user.getAudio().getData());
		if (user.getProfilePicture() != null)
			u.setProfilePicture(user.getProfilePicture().getData());
		u.setImages(user.getImages());

		u.setDonations(user.getDonations());
		u.setMessageSent(user.getMessageSent());
		// u.setWebPush(user.getWebPush());
		u.setDates(user.getDates());

		u.setNumberProfileViews(user.getNumberProfileViews());
		u.setNumberSearches(user.getNumberSearches());
		
		u.setZodiac(user.getZodiac());
		u.setShowZodiac(user.isShowZodiac());

		return u;
	}
}
