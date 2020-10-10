package com.nonononoki.alovoa.model;

import java.time.LocalDate;
import java.time.Period;
import java.util.Base64;
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
import com.nonononoki.alovoa.entity.UserInterest;
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
	
	private List<UserInterest> interests;

	private String profilePicture;
	private List<UserImage> images;

	private String description;

	private int distanceToUser;
	private int sameInterests;
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
		dto.setIdEncoded(encodeId(user.getId(), textEncryptor));
		dto.setActiveDate(user.getDates().getActiveDate());
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
		dto.setInterests(user.getInterests());
		
		dto.blockedByCurrentUser = currentUser.getBlockedUsers().stream().anyMatch(o -> o.getUserTo().getId().equals(user.getId()));
		dto.reportedByCurrentUser = currentUser.getReported().stream().anyMatch(o -> o.getUserTo().getId().equals(user.getId()));
		dto.likedByCurrentUser = currentUser.getLikes().stream().anyMatch(o -> o.getUserTo().getId().equals(user.getId()));
		dto.hiddenByCurrentUser = currentUser.getHiddenUsers().stream().anyMatch(o -> o.getUserTo().getId().equals(user.getId()));
		
		int sameInterests = 0;
		for(int i = 0; i < currentUser.getInterests().size(); i++) {
			UserInterest interest = currentUser.getInterests().get(i);
			if(user.getInterests().contains(interest)) {
				sameInterests++;
			}
		}
		dto.setSameInterests(sameInterests);
		
		double dist = 0;
		if(!currentUser.isAdmin()) {
			dist = Tools.getDistanceToUser(user, currentUser);
		}
		int distRounded = (int) Math.round(dist);
		distRounded  = distRounded - distRounded % LOCATION_ROUNDING;
		dto.setDistanceToUser(distRounded / Tools.THOUSAND); //convert meters to km
		return dto;
	}
	
	public static String encodeId(long id, TextEncryptorConverter textEncryptor) throws Exception {
		String en = textEncryptor.encode(Long.toString(id));
		en = Base64.getEncoder().encodeToString(en.getBytes());
		return en;
	}
	
	public static long decodeId(String id, TextEncryptorConverter textEncryptor) throws NumberFormatException, Exception {
		String en = new String(Base64.getDecoder().decode(id));
		long l = Long.parseLong(textEncryptor.decode(en));
		return l;
	}
}
