package com.nonononoki.alovoa.model;

import java.time.LocalDate;
import java.time.Period;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.Gender;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.UserBlock;
import com.nonononoki.alovoa.entity.UserImage;
import com.nonononoki.alovoa.entity.UserIntention;
import com.nonononoki.alovoa.entity.UserReport;

import lombok.Data;

@Data
public class UserDto {

	@JsonIgnore
	private long id;

	private String idEncoded;

	private String firstName;
	private int age;
	private float donationAmount;
	private Gender gender;
	private Set<Gender> preferedGenders;
	private UserIntention intention;

	private String profilePicture;
	private List<UserImage> images;

	private String description;

	private int distanceToUser;
	private double totalDonations;

	private Date activeDate;

	private List<UserBlock> blockedByUsers;
	private List<UserReport> reportedByUsers;

	private List<UserBlock> blockedUsers;
	
	private boolean blockedByCurrentUser;
	private boolean reportedByCurrentUser;
	private boolean likedByCurrentUser;
	private boolean hiddenByCurrentUser;
	
	private static final int LOCATION_ROUNDING = 100;

	public static UserDto userToUserDto(User user, User currentUser, TextEncryptorConverter textEncryptor)
			throws Exception {
		UserDto dto = new UserDto();
		dto.setId(user.getId());
		String en = textEncryptor.encode(Long.toString(user.getId()));
		dto.setIdEncoded(en);
		dto.setActiveDate(user.getDates().getActiveDate());
		//dto.setAge(user.getAge());
		LocalDate now = LocalDate.now();
		Period period = Period.between(user.getDates().getDateOfBirth(), now);
		dto.setAge(period.getYears());
		dto.setDescription(user.getDescription());
		dto.setFirstName(user.getFirstName());
		dto.setGender(user.getGender());
		dto.setPreferedGenders(user.getPreferedGenders());
		dto.setImages(user.getImages());
		dto.setGender(user.getGender());
		dto.setIntention(user.getIntention());
		dto.setProfilePicture(user.getProfilePicture());
		dto.setBlockedUsers(user.getBlockedUsers());
		dto.setTotalDonations(user.getTotalDonations());
		dto.setBlockedByUsers(user.getBlockedByUsers());
		dto.setReportedByUsers(user.getReportedByUsers());
		
		dto.blockedByCurrentUser = currentUser.getBlockedUsers().stream().anyMatch(o -> o.getUserTo().getId().equals(user.getId()));
		dto.reportedByCurrentUser = currentUser.getReported().stream().anyMatch(o -> o.getUserTo().getId().equals(user.getId()));
		dto.likedByCurrentUser = currentUser.getLikes().stream().anyMatch(o -> o.getUserTo().getId().equals(user.getId()));
		dto.hiddenByCurrentUser = currentUser.getHiddenUsers().stream().anyMatch(o -> o.getUserTo().getId().equals(user.getId()));
		double dist = Tools.getDistanceToUser(user, currentUser);
		int distRounded = (int) Math.round(dist);
		distRounded  = distRounded - distRounded % LOCATION_ROUNDING;
		dto.setDistanceToUser(distRounded);
		return dto;
	}
}
