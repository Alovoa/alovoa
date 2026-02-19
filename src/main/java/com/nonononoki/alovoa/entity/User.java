package com.nonononoki.alovoa.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.config.SecurityConfig;
import com.nonononoki.alovoa.entity.user.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;
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
@Getter
@Setter
@Entity
public class User implements UserDetails {

    @Transient
    public static final int UNIT_SI = 0;
    @Transient
    public static final int UNIT_IMPERIAL = 1;
    @Column(nullable = false, unique = true)
    @Convert(converter = TextEncryptorConverter.class)
    @JsonIgnore
    @Immutable
    private final String email;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique=true)
    private UUID uuid;
    @JsonIgnore
    private String password;
    @Column(updatable = false)
    @Convert(converter = TextEncryptorConverter.class)
    @Immutable
    private String firstName;
    private String description;
    // used for emails
    @JsonIgnore
    private String language;
    @JsonIgnore
    private int units;
    @JsonIgnore
    private int numberReferred;
    @JsonIgnore
    private String referrerCode;
    @JsonIgnore
    private String verificationCode;
    @JsonIgnore
    private boolean showZodiac;
    @JsonIgnore
    private boolean requireVideoFirst = false;
    // Exit Velocity tracking fields
    private java.time.LocalDate firstActiveDate;
    private java.time.LocalDate lastMeaningfulActivity;
    private java.time.LocalDate relationshipFormedDate;
    @JsonIgnore
    private boolean exitSurveyCompleted = false;
    // Account pause for intervention system
    @JsonIgnore
    private boolean accountPaused = false;
    @JsonIgnore
    @Column(name = "pause_reason", length = 100)
    private String pauseReason;
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
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "user")
    private UserVideoIntroduction videoIntroduction;

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
    @OneToMany(cascade = CascadeType.PERSIST, orphanRemoval = true, mappedBy = "userFrom")
    @JsonIgnore
    private List<Message> messageSent;
    @OneToMany(orphanRemoval = true, mappedBy = "userTo")
    @JsonIgnore
    private List<Message> messageReceived;
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, mappedBy = "users")
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
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
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    @JoinTable
    @JsonIgnore
    private List<UserVerificationPicture> verificationNo;
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    @JoinTable
    @JsonIgnore
    private List<UserVerificationPicture> verificationYes;
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private UserSettings userSettings;

    // AURA Extensions
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "user")
    private List<UserVideo> videos;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn
    private UserVideoVerification videoVerification;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn
    private UserReputationScore reputationScore;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "user")
    @JsonIgnore
    private List<UserBehaviorEvent> behaviorEvents;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn
    private UserPersonalityProfile personalityProfile;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "user")
    @JsonIgnore
    private List<UserDailyMatchLimit> dailyMatchLimits;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "userA")
    @JsonIgnore
    private List<VideoDate> videoDatesInitiated;

    @OneToMany(mappedBy = "userB")
    @JsonIgnore
    private List<VideoDate> videoDatesReceived;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn
    private UserPoliticalAssessment politicalAssessment;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "subject")
    @JsonIgnore
    private List<UserAccountabilityReport> accountabilityReportsReceived;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "reporter")
    @JsonIgnore
    private List<UserAccountabilityReport> accountabilityReportsSubmitted;

    // OKCupid 2016 feature parity
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn
    private UserProfileDetails profileDetails;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "visitedUser")
    @JsonIgnore
    private List<UserProfileVisit> profileVisitors;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "visitor")
    @JsonIgnore
    private List<UserProfileVisit> profileVisits;

    // Privacy-safe location system
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "user")
    private List<UserLocationArea> locationAreas;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn
    private UserLocationPreferences locationPreferences;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn
    private UserTravelingMode travelingMode;

    // Donation tracking (donation-only model)
    @Column(name = "donation_tier")
    @Enumerated(EnumType.STRING)
    private DonationTier donationTier = DonationTier.NONE;

    @Column(name = "last_donation_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastDonationDate;

    @Column(name = "donation_streak_months")
    private int donationStreakMonths = 0;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "user")
    @JsonIgnore
    private List<DonationPrompt> donationPrompts;

    // Intervention system
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "user")
    @JsonIgnore
    private AccountPause currentPause;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "user")
    @JsonIgnore
    private List<InterventionDelivery> interventionDeliveries;

    public enum DonationTier {
        NONE,           // Never donated
        SUPPORTER,      // $5-20 - Thank you email
        BELIEVER,       // $21-50 - Name in supporters page (optional)
        BUILDER,        // $51-100 - Early access, founder badge on profile
        FOUNDING_MEMBER // $100+ - All above + quarterly updates, feature input
    }

    // AURA Helper methods
    public boolean isVideoVerified() {
        return videoVerification != null && videoVerification.isVerified();
    }

    public UserReputationScore.TrustLevel getTrustLevel() {
        if (reputationScore == null) {
            return UserReputationScore.TrustLevel.NEW_MEMBER;
        }
        return reputationScore.getTrustLevel();
    }

    public Double getReputationOverall() {
        if (reputationScore == null) return 50.0;
        return reputationScore.getOverallScore();
    }

    public UserVideo getIntroVideo() {
        if (videos == null) return null;
        return videos.stream()
            .filter(v -> Boolean.TRUE.equals(v.getIsIntro()))
            .findFirst()
            .orElse(null);
    }

    public List<Conversation> getConversations() {
        return conversations == null ? null : List.copyOf(conversations);
    }

    public void setConversations(List<Conversation> conversations) {
        this.conversations = conversations == null ? null : new ArrayList<>(conversations);
    }

    public void addConversation(Conversation conversation) {
        if (conversation == null) {
            return;
        }
        if (conversations == null) {
            conversations = new ArrayList<>();
        }
        if (!conversations.contains(conversation)) {
            conversations.add(conversation);
        }
    }

    public void removeConversation(Conversation conversation) {
        if (conversation == null || conversations == null) {
            return;
        }
        conversations.remove(conversation);
    }

    public boolean isPoliticalAssessmentComplete() {
        return politicalAssessment != null &&
               politicalAssessment.getGateStatus() != UserPoliticalAssessment.GateStatus.PENDING_ASSESSMENT;
    }

    public boolean isPoliticallyApproved() {
        return politicalAssessment != null &&
               politicalAssessment.getGateStatus() == UserPoliticalAssessment.GateStatus.APPROVED;
    }

    public UserPoliticalAssessment.GateStatus getPoliticalGateStatus() {
        if (politicalAssessment == null) {
            return UserPoliticalAssessment.GateStatus.PENDING_ASSESSMENT;
        }
        return politicalAssessment.getGateStatus();
    }

    public int getPublicFeedbackCount() {
        if (accountabilityReportsReceived == null) return 0;
        return (int) accountabilityReportsReceived.stream()
            .filter(r -> r.getStatus() == UserAccountabilityReport.ReportStatus.PUBLISHED)
            .count();
    }

    public int getPositiveFeedbackCount() {
        if (accountabilityReportsReceived == null) return 0;
        return (int) accountabilityReportsReceived.stream()
            .filter(r -> r.getStatus() == UserAccountabilityReport.ReportStatus.PUBLISHED)
            .filter(r -> r.getCategory() == UserAccountabilityReport.AccountabilityCategory.POSITIVE_EXPERIENCE)
            .count();
    }

    public int getNegativeFeedbackCount() {
        if (accountabilityReportsReceived == null) return 0;
        return (int) accountabilityReportsReceived.stream()
            .filter(r -> r.getStatus() == UserAccountabilityReport.ReportStatus.PUBLISHED)
            .filter(r -> r.getCategory() != UserAccountabilityReport.AccountabilityCategory.POSITIVE_EXPERIENCE)
            .count();
    }

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
