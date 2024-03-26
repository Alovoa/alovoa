package com.nonononoki.alovoa.model;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.*;
import lombok.Data;

import java.util.List;
import java.util.Set;

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
    private String verificationPicture;
    private String audio;
    private String language;
    private String accentColor;
    private String uiDesign;
    private String country;
    private int units;
    private Gender gender;
    private UserIntention intention;
    private List<UserInterest> interests;
    private Set<Gender> preferedGenders;
    private List<UserImage> images;
    private List<UserPrompt> prompts;
    private List<UserDonation> donations;
    private List<Message> messageSent;
    private UserDates dates;
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
        u.setCountry(user.getCountry());
        u.setUnits(user.getUnits());

        u.setLocationLatitude(user.getLocationLatitude());
        u.setLocationLongitude(user.getLocationLongitude());

        u.setGender(user.getGender());
        u.setIntention(user.getIntention());
        u.setInterests(user.getInterests());
        u.setPreferedGenders(user.getPreferedGenders());

        if (user.getAudio() != null) {
            u.setAudio(user.getAudio().getData());
        }
        if (user.getProfilePicture() != null) {
            u.setProfilePicture(user.getProfilePicture().getData());
        }
        if (user.getVerificationPicture() != null) {
            u.setVerificationPicture(user.getVerificationPicture().getData());
        }
        u.setImages(user.getImages());

        u.setDonations(user.getDonations());
        u.setMessageSent(user.getMessageSent());
        u.setDates(user.getDates());

        u.setShowZodiac(user.isShowZodiac());
        u.setPrompts(user.getPrompts());

        return u;
    }
}
