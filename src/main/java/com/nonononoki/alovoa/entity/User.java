package com.nonononoki.alovoa.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.config.SecurityConfig;
import com.nonononoki.alovoa.entity.user.*;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.*;

@Component
@Data
@Entity
public class User implements UserDetails {

    @Transient
    public static final String ACCENT_COLOR_PINK = "pink";
    @Transient
    public static final String ACCENT_COLOR_BLUE = "blue";
    @Transient
    public static final String ACCENT_COLOR_ORANGE = "orange";
    @Transient
    public static final String ACCENT_COLOR_PURPLE = "purple";
    @Transient
    public static final int UNIT_SI = 0;
    @Transient
    public static final int UNIT_IMPERIAL = 1;
    @Column(nullable = false, unique = true, updatable = false)
    @Convert(converter = TextEncryptorConverter.class)
    @JsonIgnore
    private final String email;
    // some more data
    @JsonIgnore
    long numberProfileViews;
    @JsonIgnore
    long numberSearches;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JsonIgnore
    private String password;
    @Column(updatable = false)
    @Convert(converter = TextEncryptorConverter.class)
    private String firstName;
    private String description;
    // used for emails
    @JsonIgnore
    private String language;
    @JsonIgnore
    private String accentColor;
    @JsonIgnore
    private int units;
    @JsonIgnore
    private int numberReferred;
    @JsonIgnore
    private String referrerCode;
    @JsonIgnore
    private String verificationCode;
    @JsonIgnore
    private String uiDesign;
    @JsonIgnore
    private boolean showZodiac;
    private int preferedMinAge;
    private int preferedMaxAge;
    @JsonIgnore
    private Double locationLatitude;
    @JsonIgnore
    private Double locationLongitude;
    private double totalDonations;
    @JsonIgnore
    private boolean admin;
    @JsonIgnore
    private boolean confirmed;
    @JsonIgnore
    private boolean disabled;
    @JsonIgnore
    private String country;
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn
    @JsonIgnore
    private UserRegisterToken registerToken;
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn
    @JsonIgnore
    private UserPasswordToken passwordToken;
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn
    @JsonIgnore
    private UserDeleteToken deleteToken;
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn
    @JsonIgnore
    private UserDates dates;
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn
    private UserAudio audio;
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn
    private UserProfilePicture profilePicture;
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn
    private UserVerificationPicture verificationPicture;

    // Tables with multiple users
    @ManyToOne
    private Gender gender;
    @ManyToMany
    @JoinTable(name = "user2genders")
    private Set<Gender> preferedGenders;
    @ManyToMany
    private Set<UserMiscInfo> miscInfos;
    @ManyToOne
    private UserIntention intention;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "user")
    private List<UserInterest> interests;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "user")
    private List<UserPrompt> prompts;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "user")
    private List<UserImage> images;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "user")
    @JsonIgnore
    private List<UserDonation> donations;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "user")
    @JsonIgnore
    private List<UserWebPush> webPush;
    @OneToMany(cascade = CascadeType.PERSIST, orphanRemoval = true, mappedBy = "userFrom")
    @JsonIgnore
    private List<Message> messageSent;
    @OneToMany(orphanRemoval = true, mappedBy = "userTo")
    @JsonIgnore
    private List<Message> messageReceived;
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, mappedBy = "users")
    @JsonIgnore
    private List<Conversation> conversations;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userFrom")
    @JsonIgnore
    private List<UserLike> likes;
    @OneToMany(orphanRemoval = true, mappedBy = "userTo")
    @JsonIgnore
    private List<UserLike> likedBy;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userTo")
    @JsonIgnore
    private List<UserNotification> notifications;
    @OneToMany(orphanRemoval = true, mappedBy = "userFrom")
    @JsonIgnore
    private List<UserNotification> notificationsFrom;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userFrom")
    private List<UserHide> hiddenUsers;
    @OneToMany(orphanRemoval = true, mappedBy = "userTo")
    @JsonIgnore
    private List<UserHide> hiddenByUsers;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userFrom")
    @JsonIgnore
    private List<UserBlock> blockedUsers;
    @OneToMany(orphanRemoval = true, mappedBy = "userTo")
    @JsonIgnore
    private List<UserBlock> blockedByUsers;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userFrom")
    @JsonIgnore
    private List<UserReport> reported;
    @OneToMany(orphanRemoval = true, mappedBy = "userTo")
    @JsonIgnore
    private List<UserReport> reportedByUsers;
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, mappedBy = "userNo")
    @JsonIgnore
    private List<UserVerificationPicture> verificationNo;
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, mappedBy = "userYes")
    @JsonIgnore
    private List<UserVerificationPicture> verificationYes;
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private UserSettings userSettings;

    @Deprecated
    public User() {
        email = null;
    }

    public User(String email) {
        this.email = email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        String role;
        if (admin) {
            role = SecurityConfig.getRoleAdmin();
        } else {
            role = SecurityConfig.getRoleUser();
        }
        authorities.add(new SimpleGrantedAuthority(role));

        return authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return !disabled;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !disabled;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return !disabled;
    }

    @Override
    public boolean isEnabled() {
        return !disabled;
    }

    public UserSettings getUserSettings() {
        return Objects.requireNonNullElseGet(userSettings, () -> new UserSettings(this));
    }

    public int getPreferedMinAge() {
        try {
            return Tools.convertPrefAgeToExactYear(this.getDates().getDateOfBirth(), preferedMinAge);
        } catch (Exception e) {
            return 0;
        }
    }

    public void setPreferedMinAge(int preferedMinAge) {
        try {
            this.preferedMinAge = Tools.convertPrefAgeToRelativeYear(this.getDates().getDateOfBirth(), preferedMinAge);
        } catch (Exception ignored) {
        }
    }

    public int getPreferedMaxAge() {
        try {
            return Tools.convertPrefAgeToExactYear(this.getDates().getDateOfBirth(), preferedMaxAge);
        } catch (Exception e) {
            return 0;
        }
    }

    public void setPreferedMaxAge(int preferedMaxAge) {
        try {
            this.preferedMaxAge = Tools.convertPrefAgeToRelativeYear(this.getDates().getDateOfBirth(), preferedMaxAge);
        } catch (Exception ignored) {
        }
    }

    public void setPreferedMinAge(Date dob, int preferedMinAge) {
        this.preferedMinAge = Tools.convertPrefAgeToRelativeYear(dob, preferedMinAge);
    }

    public void setPreferedMaxAge(Date dob, int preferedMaxAge) {
        this.preferedMaxAge = Tools.convertPrefAgeToRelativeYear(dob, preferedMaxAge);
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        throw new IOException();
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        throw new IOException();
    }

}
