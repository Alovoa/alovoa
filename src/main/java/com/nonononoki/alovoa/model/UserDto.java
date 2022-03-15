package com.nonononoki.alovoa.model;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.component.AuthProvider;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.Gender;
import com.nonononoki.alovoa.entity.user.UserBlock;
import com.nonononoki.alovoa.entity.user.UserImage;
import com.nonononoki.alovoa.entity.user.UserIntention;
import com.nonononoki.alovoa.entity.user.UserInterest;
import com.nonononoki.alovoa.entity.user.UserMiscInfo;
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

	private Set<UserMiscInfo> miscInfos;
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

	private boolean hasLocation;

	private int lastActiveState = 5;

	public static final int ALL = 0;
	public static final int PROFILE_PICTURE_ONLY = 1;
	public static final int NO_AUDIO = 2;
	public static final int NO_MEDIA = 3;

	// in minutes
	public static final int LA_STATE_ACTIVE_1 = 5;
	public static final int LA_STATE_ACTIVE_2 = 1;
	public static final int LA_STATE_ACTIVE_3 = 3;
	public static final int LA_STATE_ACTIVE_4 = 7;

	private static final double MILES_TO_KM = 0.6214;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(UserDto.class);

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
		if (user.getLocationLatitude() != null) {
			dto.setHasLocation(true);
		}
		dto.setDescription(user.getDescription());
		dto.setFirstName(user.getFirstName());
		dto.setGender(user.getGender());
		dto.setAccentColor(user.getAccentColor());
		dto.setUiDesign(user.getUiDesign());
		if (currentUser.isShowZodiac()) {
			dto.setZodiac(getUserZodiac(user));
		}
		dto.setShowZodiac(user.isShowZodiac());
		dto.setUnits(user.getUnits());
		dto.setMiscInfos(user.getMiscInfos());
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

		if (!user.isAdmin()) {
			LocalDateTime now = LocalDateTime.now();
			LocalDateTime activeDateTime = Tools.dateToLocalDateTime(user.getDates().getActiveDate());
			if (activeDateTime.isAfter(now.minusMinutes(LA_STATE_ACTIVE_1))) {
				dto.setLastActiveState(1);
			} else if (activeDateTime.isAfter(now.minusDays(LA_STATE_ACTIVE_2))) {
				dto.setLastActiveState(2);
			} else if (activeDateTime.isAfter(now.minusDays(LA_STATE_ACTIVE_3))) {
				dto.setLastActiveState(3);
			} else if (activeDateTime.isAfter(now.minusDays(LA_STATE_ACTIVE_4))) {
				dto.setLastActiveState(4);
			}
		}

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

			int dist = 99999;
			if (!currentUser.isAdmin()) {
				dist = Tools.getDistanceToUser(user, currentUser);
				if (currentUser.getUnits() == User.UNIT_IMPERIAL) {
					dist = (int) (dist * MILES_TO_KM);
				}
				if(dist == 0) {
					LOGGER.warn("ZERO DISTANCE: User1 [ID, LAT, LONG]: ".concat(currentUser.getId().toString()).concat(",").concat(currentUser.getLocationLatitude().toString()).concat(",").concat(currentUser.getLocationLongitude().toString()));
					LOGGER.warn("ZERO DISTANCE: User2 [ID, LAT, LONG]: ".concat(user.getId().toString()).concat(",").concat(user.getLocationLatitude().toString()).concat(",").concat(user.getLocationLongitude().toString()));
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

	public static long decodeId(String id, TextEncryptorConverter textEncryptor) throws NumberFormatException,
			InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidAlgorithmParameterException, NumberFormatException {
		String en = new String(Base64.getDecoder().decode(id));
		return Long.parseLong(textEncryptor.decode(en));
	}

	public static String getUserZodiac(User user) {
		try {
			Calendar cal = Calendar.getInstance();
			cal.setTime(user.getDates().getDateOfBirth());
			int month = cal.get(Calendar.MONTH) + 1;
			int day = cal.get(Calendar.DAY_OF_MONTH);
			if ((month == 12 && day >= 22 && day <= 31) || (month == 1 && day >= 1 && day <= 19))
				return "capricorn";
			else if ((month == 1 && day >= 20 && day <= 31) || (month == 2 && day >= 1 && day <= 17))
				return "aquarius";
			else if ((month == 2 && day >= 18 && day <= 29) || (month == 3 && day >= 1 && day <= 19))
				return "pisces";
			else if ((month == 3 && day >= 20 && day <= 31) || (month == 4 && day >= 1 && day <= 19))
				return "aries";
			else if ((month == 4 && day >= 20 && day <= 30) || (month == 5 && day >= 1 && day <= 20))
				return "taurus";
			else if ((month == 5 && day >= 21 && day <= 31) || (month == 6 && day >= 1 && day <= 20))
				return "gemini";
			else if ((month == 6 && day >= 21 && day <= 30) || (month == 7 && day >= 1 && day <= 22))
				return "cancer";
			else if ((month == 7 && day >= 23 && day <= 31) || (month == 8 && day >= 1 && day <= 22))
				return "leo";
			else if ((month == 8 && day >= 23 && day <= 31) || (month == 9 && day >= 1 && day <= 22))
				return "virgo";
			else if ((month == 9 && day >= 23 && day <= 30) || (month == 10 && day >= 1 && day <= 22))
				return "libra";
			else if ((month == 10 && day >= 23 && day <= 31) || (month == 11 && day >= 1 && day <= 21))
				return "scorpio";
			else if ((month == 11 && day >= 22 && day <= 30) || (month == 12 && day >= 1 && day <= 21))
				return "sagittarius";
		} finally {
		}
		return null;

	}
}
