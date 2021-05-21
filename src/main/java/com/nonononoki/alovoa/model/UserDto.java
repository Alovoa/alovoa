package com.nonononoki.alovoa.model;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.Gender;
import com.nonononoki.alovoa.entity.user.UserBlock;
import com.nonononoki.alovoa.entity.user.UserImage;
import com.nonononoki.alovoa.entity.user.UserIntention;
import com.nonononoki.alovoa.entity.user.UserInterest;
import com.nonononoki.alovoa.entity.user.UserReport;

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

	private boolean hasAudio;
	private String audio;
	
	//private String language;
	private String accentColor;
	private String uiDesign;
	
	private int preferedMinAge;
	private int preferedMaxAge;

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
	
	long numberProfileViews;
	long numberSearches;

	public static int ALL = 0;
	public static int PROFILE_PICTURE_ONLY = 1;
	public static int NO_AUDIO = 2;
	public static int NO_MEDIA = 3;

	public static UserDto userToUserDto(User user, User currentUser, TextEncryptorConverter textEncryptor)
			throws Exception, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException {
		return userToUserDto(user, currentUser, textEncryptor, NO_AUDIO);
	}

	public static UserDto userToUserDto(User user, User currentUser, TextEncryptorConverter textEncryptor, int mode)
			throws Exception, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException {
		
		if(user == null) {
			return null;
		}
		UserDto dto = new UserDto();
		dto.setId(user.getId());
		dto.setIdEncoded(encodeId(user.getId(), textEncryptor));
		dto.setActiveDate(user.getDates().getActiveDate());
		dto.setAge(Tools.calcUserAge(user));
		dto.setDescription(user.getDescription());
		dto.setFirstName(user.getFirstName());
		dto.setGender(user.getGender());
		dto.setAccentColor(user.getAccentColor());
		dto.setUiDesign(user.getUiDesign());
		dto.setPreferedGenders(user.getPreferedGenders());
		dto.setPreferedMinAge(user.getPreferedMinAge());
		dto.setPreferedMaxAge(user.getPreferedMaxAge());
		if(mode != PROFILE_PICTURE_ONLY) {
			dto.setImages(user.getImages());
		}
		dto.setGender(user.getGender());
		dto.setIntention(user.getIntention());
		if(user.getProfilePicture() != null) {
			dto.setProfilePicture(user.getProfilePicture().getData());
		}
		dto.setBlockedUsers(user.getBlockedUsers());
		dto.setTotalDonations(user.getTotalDonations());
		dto.setBlockedByUsers(user.getBlockedByUsers());
		dto.setReportedByUsers(user.getReportedByUsers());
		dto.setInterests(user.getInterests());
		if((mode != NO_AUDIO || mode != PROFILE_PICTURE_ONLY) && user.getAudio() != null) {
			dto.setAudio(user.getAudio().getData());
		}
		dto.setHasAudio(user.getAudio() != null);
		dto.setNumberProfileViews(user.getNumberProfileViews());
		dto.setNumberSearches(user.getNumberSearches());
		
		if(!user.equals(currentUser)) {
			dto.blockedByCurrentUser = currentUser.getBlockedUsers().stream()
					.anyMatch(o -> o.getUserTo().getId().equals(user.getId()));
			dto.reportedByCurrentUser = currentUser.getReported().stream()
					.anyMatch(o -> o.getUserTo().getId().equals(user.getId()));
			dto.likedByCurrentUser = currentUser.getLikes().stream()
					.anyMatch(o -> o.getUserTo().getId().equals(user.getId()));
			dto.hiddenByCurrentUser = currentUser.getHiddenUsers().stream()
					.anyMatch(o -> o.getUserTo().getId().equals(user.getId()));
	
			int sameInterests = 0;
			for (int i = 0; i < currentUser.getInterests().size(); i++) {
				UserInterest interest = currentUser.getInterests().get(i);
				if (user.getInterests().contains(interest)) {
					sameInterests++;
				}
			}
			dto.setSameInterests(sameInterests);
	
			int dist = 0;
			if (!currentUser.isAdmin()) {
				dist = Tools.getDistanceToUser(user, currentUser);
			}
			dto.setDistanceToUser(dist);
		}
		return dto;
	}

	public static String encodeId(long id, TextEncryptorConverter textEncryptor) throws Exception, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException {
		String en = textEncryptor.encode(Long.toString(id));
		en = Base64.getEncoder().encodeToString(en.getBytes("UTF-8"));
		return en;
	}

	public static long decodeId(String id, TextEncryptorConverter textEncryptor)
			throws NumberFormatException, Exception {
		String en = new String(Base64.getDecoder().decode(id));
		long l = Long.parseLong(textEncryptor.decode(en));
		return l;
	}
}
