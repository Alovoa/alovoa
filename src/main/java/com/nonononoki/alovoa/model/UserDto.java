package com.nonononoki.alovoa.model;

import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.Gender;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.UserBlock;
import com.nonononoki.alovoa.entity.UserImage;
import com.nonononoki.alovoa.entity.UserIntention;

import lombok.Data;

@Data
@Component
public class UserDto {

	@JsonIgnore
	private long id;

	private String idEncoded;

	private String firstName;
	private int age;
	private Gender gender;
	private Set<Gender> preferedGenders;
	private UserIntention intention;

	private String profilePicture;
	private List<UserImage> images;

	private String description;

	private int distanceToUser;
	private double totalDonations;

	private Date activeDate;

	private long numberOfBlocks;
	private long numberOfReports;
	
	private List<UserBlock> blockedUsers;

	public static UserDto userToUserDto(User user, User currentUser, TextEncryptorConverter textEncryptor) throws Exception {
		UserDto dto = new UserDto();
		dto.setId(user.getId());
		String en = textEncryptor.encode(Long.toString(user.getId()));
		dto.setIdEncoded(en);
		dto.setActiveDate(user.getActiveDate());
		dto.setAge(user.getAge());
		dto.setDescription(user.getDescription());
		dto.setFirstName(user.getFirstName());
		dto.setGender(user.getGender());
		dto.setPreferedGenders(user.getPreferedGenders());
		dto.setImages(user.getImages());
		dto.setGender(user.getGender());
		dto.setIntention(user.getIntention());
		dto.setProfilePicture(user.getProfilePicture());
		dto.setBlockedUsers(user.getBlockedUsers());
		try {
			dto.setNumberOfReports(user.getReportedByUsers().size());
		} catch (Exception e) {
			dto.setNumberOfReports(0);
		}
		try {
			dto.setNumberOfBlocks(user.getBlockedByUsers().size());
		} catch (Exception e) {
			dto.setNumberOfBlocks(0);
		}
		double donations = 0;
		for (int i = 0; user.getDonations() != null && i < user.getDonations().size(); i++) {
			donations += user.getDonations().get(i).getAmount();
		}
		dto.setTotalDonations(donations);
		double dist = Tools.getDistanceToUser(user, currentUser);
		dto.setDistanceToUser((int) Math.round(dist));
		return dto;
	}
}
