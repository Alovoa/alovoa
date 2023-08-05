package com.nonononoki.alovoa.model;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.*;
import com.nonononoki.alovoa.service.UserService;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

@Data
public class UserDto {
    public static final int ALL = 0;
    public static final int PROFILE_PICTURE_ONLY = 1;
    public static final int NO_AUDIO = 2;
    public static final int NO_MEDIA = 3; //always send verification picture unless verified by admin
    public static final int LA_STATE_ACTIVE_1 = 5; // in minutes
    public static final int LA_STATE_ACTIVE_2 = 1;
    public static final int LA_STATE_ACTIVE_3 = 7;
    public static final int LA_STATE_ACTIVE_4 = 30;
    public static final int VERIFICATION_MINUMUM = 5;
    public static final int VERIFICATION_FACTOR = 5;
    private static final double MILES_TO_KM = 0.6214;
    private static final Logger LOGGER = LoggerFactory.getLogger(UserDto.class);

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
    private List<UserInterest> commonInterests;
    private String profilePicture;
    private List<UserImage> images;
    private String description;
    private String country;
    private int distanceToUser;
    private double totalDonations;
    private long numBlockedByUsers;
    private long numReports;
    private boolean blockedByCurrentUser;
    private boolean reportedByCurrentUser;
    private boolean likedByCurrentUser;
    private boolean hiddenByCurrentUser;
    private long numberReferred;
    private long numberProfileViews;
    private long numberSearches;
    private boolean compatible;
    private boolean hasLocation;
    private Double locationLatitude;
    private Double locationLongitude;
    private UserDtoVerificationPicture verificationPicture;
    private int lastActiveState = 5;

    public static UserDto userToUserDto(User user, User currentUser, UserService userService, TextEncryptorConverter textEncryptor)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
        return userToUserDto(user, currentUser, userService, textEncryptor, NO_AUDIO);
    }

    public static UserDto userToUserDto(User user, User currentUser, UserService userService, TextEncryptorConverter textEncryptor, int mode)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
        if (user == null) {
            return null;
        }
        UserDto dto = new UserDto();
        if (user.equals(currentUser)) {
            dto.setEmail(user.getEmail());
            dto.setLocationLatitude(user.getLocationLatitude());
            dto.setLocationLongitude(user.getLocationLongitude());
            dto.setAccentColor(user.getAccentColor());
            dto.setUiDesign(user.getUiDesign());
        }
        dto.setIdEncoded(encodeId(user.getId(), textEncryptor));
        if (user.getDates() != null) {
            dto.setAge(Tools.calcUserAge(user));
        }
        if (user.getLocationLatitude() != null) {
            dto.setHasLocation(true);
        }

        dto.setDescription(user.getDescription());
        dto.setFirstName(user.getFirstName());
        dto.setGender(user.getGender());
        dto.setVerificationPicture(UserDtoVerificationPicture.map(user, currentUser, userService, mode));

        dto.setCountry(Tools.getCountryEmoji(user.getCountry()));

        if (currentUser.isShowZodiac()) {
            dto.setZodiac(getUserZodiac(user));
        }
        dto.setShowZodiac(user.isShowZodiac());
        dto.setUnits(user.getUnits());
        dto.setMiscInfos(user.getMiscInfos());
        dto.setPreferedGenders(user.getPreferedGenders());
        dto.setPreferedMinAge(user.getPreferedMinAge());
        dto.setPreferedMaxAge(user.getPreferedMaxAge());
        if (dto.getPreferedMinAge() < Tools.AGE_LEGAL && dto.getAge() >= Tools.AGE_LEGAL) {
            dto.setPreferedMinAge(Tools.AGE_LEGAL);
        }
        if (mode != PROFILE_PICTURE_ONLY && mode != NO_MEDIA) {
            dto.setImages(user.getImages());
        }
        dto.setGender(user.getGender());
        dto.setIntention(user.getIntention());
        if (mode != NO_MEDIA && user.getProfilePicture() != null) {
            dto.setProfilePicture(user.getProfilePicture().getData());
        }
        dto.setTotalDonations(user.getTotalDonations());
        dto.setNumBlockedByUsers(user.getBlockedByUsers().size());
        dto.setNumReports(user.getReportedByUsers().size());
        dto.setInterests(user.getInterests());
        if ((mode != NO_AUDIO && mode != PROFILE_PICTURE_ONLY && mode != NO_MEDIA) && user.getAudio() != null) {
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
            if (currentUser.getBlockedUsers() != null) {
                dto.blockedByCurrentUser = currentUser.getBlockedUsers().stream()
                        .anyMatch(o -> o.getUserTo().getId().equals(user.getId()));
            }
            if (currentUser.getReported() != null) {
                dto.reportedByCurrentUser = currentUser.getReported().stream()
                        .anyMatch(o -> o.getUserTo().getId().equals(user.getId()));
            }
            if (currentUser.getLikes() != null) {
                dto.likedByCurrentUser = currentUser.getLikes().stream()
                        .anyMatch(o -> o.getUserTo().getId().equals(user.getId()));
            }
            if (currentUser.getHiddenUsers() != null) {
                dto.hiddenByCurrentUser = currentUser.getHiddenUsers().stream()
                        .anyMatch(o -> o.getUserTo().getId().equals(user.getId()));
            }

            List<UserInterest> commonInterests = new ArrayList<>();
            for (int i = 0; i < currentUser.getInterests().size(); i++) {
                UserInterest interest = currentUser.getInterests().get(i);
                if (user.getInterests().contains(interest)) {
                    commonInterests.add(interest);
                }
            }
            dto.setCommonInterests(commonInterests);

            int dist = 99999;
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
            NoSuchPaddingException, InvalidAlgorithmParameterException {
        return Base64.getEncoder()
                .encodeToString(textEncryptor.encode(Long.toString(id)).getBytes(StandardCharsets.UTF_8));
    }

    public static long decodeIdThrowing(String id, TextEncryptorConverter textEncryptor)
            throws NumberFormatException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
            NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {
        String en = new String(Base64.getDecoder().decode(id));
        return Long.parseLong(textEncryptor.decode(en));
    }

    public static Optional<Long> decodeId(String id, TextEncryptorConverter textEncryptor) {
        try {
            String en = new String(Base64.getDecoder().decode(id));
            return Optional.of(Long.parseLong(textEncryptor.decode(en)));
        } catch (Exception e) {
            LOGGER.debug(String.format("Couldn't decode id '%s'", id), e);
        }
        return Optional.empty();
    }

    public static String getUserZodiac(User user) {
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
        return null;

    }

    public static boolean isVerifiedByUsers(UserVerificationPicture pic) {
        if (pic.getUserYes().size() < VERIFICATION_MINUMUM) {
            return false;
        }
        return pic.getUserNo().size() * VERIFICATION_FACTOR <= pic.getUserYes().size();
    }

    @Data
    public static class UserDtoVerificationPicture {
        private boolean verifiedByAdmin;
        private boolean verifiedByUsers;
        private boolean votedByCurrentUser;
        private boolean hasPicture;
        private String data;
        private String text;
        private int userYes;
        private int userNo;

        public static UserDtoVerificationPicture map(User user, User currentUser, UserService userService, int mode) {
            UserDtoVerificationPicture verificationPicture = new UserDtoVerificationPicture();
            verificationPicture.setText(userService.getVerificationCode(user));
            UserVerificationPicture pic = user.getVerificationPicture();
            verificationPicture.setHasPicture(pic != null && pic.getData() != null);

            if (pic == null) {
                return verificationPicture;
            }

            if (mode == ALL && !pic.isVerifiedByAdmin()) {
                verificationPicture.setData(pic.getData());
            }

            //only show verification for users with verification
            if (currentUser == user || currentUser.getVerificationPicture() == null && !currentUser.isAdmin()) {
                return verificationPicture;
            }

            verificationPicture.setUserNo(pic.getUserNo().size());
            verificationPicture.setUserYes(pic.getUserYes().size());
            verificationPicture.setVerifiedByUsers(UserDto.isVerifiedByUsers(pic));
            verificationPicture.setVerifiedByAdmin(pic.isVerifiedByAdmin());
            verificationPicture.setVotedByCurrentUser(pic.getUserYes().contains(currentUser) || pic.getUserNo().contains(currentUser));

            return verificationPicture;
        }
    }
}
