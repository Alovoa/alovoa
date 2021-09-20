package com.nonononoki.alovoa.model;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
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

	private String email;

	private String firstName;
	private int age;
	private float donationAmount;
	private Gender gender;

	private boolean hasAudio;
	private String audio;

	private String accentColor;
	private String uiDesign;
	private String zodiac;
	private boolean showZodiac;
	private int units;

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

	private long numberReferred;
	private long numberProfileViews;
	private long numberSearches;

	private boolean compatible;

	public static final int ALL = 0;
	public static final int PROFILE_PICTURE_ONLY = 1;
	public static final int NO_AUDIO = 2;
	public static final int NO_MEDIA = 3;

	private static final double MILES_TO_KM = 0.6214;

	public static UserDto userToUserDto(User user, User currentUser, TextEncryptorConverter textEncryptor)
			throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException {
		return userToUserDto(user, currentUser, textEncryptor, NO_AUDIO);
	}

	public static UserDto userToUserDto(User user, User currentUser, TextEncryptorConverter textEncryptor, int mode)
			throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException {
		if (user == null) {
			return null;
		}
		UserDto dto = new UserDto();
		dto.setId(user.getId());
		dto.setEmail(user.getEmail());
		dto.setIdEncoded(encodeId(user.getId(), textEncryptor));
		if (user.getDates() != null) {
			dto.setActiveDate(user.getDates().getActiveDate());
			dto.setAge(Tools.calcUserAge(user));
		}
		dto.setDescription(user.getDescription());
		dto.setFirstName(user.getFirstName());
		dto.setGender(user.getGender());
		dto.setAccentColor(user.getAccentColor());
		dto.setUiDesign(user.getUiDesign());
		dto.setZodiac(user.getZodiac());
		dto.setShowZodiac(user.isShowZodiac());
		dto.setUnits(user.getUnits());
		dto.setPreferedGenders(user.getPreferedGenders());
		dto.setPreferedMinAge(user.getPreferedMinAge());
		dto.setPreferedMaxAge(user.getPreferedMaxAge());
		if (mode != PROFILE_PICTURE_ONLY) {
			dto.setImages(user.getImages());
		}
		dto.setGender(user.getGender());
		dto.setIntention(user.getIntention());
		if (user.getProfilePicture() != null) {
			dto.setProfilePicture(user.getProfilePicture().getData());
		}
		dto.setBlockedUsers(user.getBlockedUsers());
		dto.setTotalDonations(user.getTotalDonations());
		dto.setBlockedByUsers(user.getBlockedByUsers());
		dto.setReportedByUsers(user.getReportedByUsers());
		dto.setInterests(user.getInterests());
		if ((mode != NO_AUDIO || mode != PROFILE_PICTURE_ONLY) && user.getAudio() != null) {
			dto.setAudio(user.getAudio().getData());
		}
		dto.setHasAudio(user.getAudio() != null);
		dto.setNumberReferred(user.getNumberReferred());
		dto.setNumberProfileViews(user.getNumberProfileViews());
		dto.setNumberSearches(user.getNumberSearches());

		if (!user.equals(currentUser)) {
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
				if (currentUser.getUnits() == User.UNIT_IMPERIAL) {
					dist = (int) (dist * MILES_TO_KM);
				}
			}
			dto.setDistanceToUser(dist);
		}
		dto.setCompatible(Tools.usersCompatible(currentUser, user));
		return dto;
	}

	public static String encodeId(long id, TextEncryptorConverter textEncryptor)
			throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException {
		return Base64.getEncoder()
				.encodeToString(textEncryptor.encode(Long.toString(id)).getBytes(StandardCharsets.UTF_8.name()));
	}

	public static long decodeId(String id, TextEncryptorConverter textEncryptor)
			throws NumberFormatException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
			NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, NumberFormatException {
		String en = new String(Base64.getDecoder().decode(id));
		return Long.parseLong(textEncryptor.decode(en));
	}
}
