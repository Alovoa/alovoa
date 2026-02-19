package com.nonononoki.alovoa.model;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserReputationScore;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * DTO for displaying user reputation information to other users.
 * Contains sanitized reputation data that is safe to show publicly.
 */
@Getter
@Setter
public class ReputationDisplayDto {

    private String trustLevel;
    private String trustLevelDescription;
    private int overallScore;
    private int responseQuality;
    private int respectScore;
    private int authenticityScore;
    private int investmentScore;
    private int positiveFeedbackCount;
    private int datesCompleted;
    private boolean videoVerified;
    private String memberSince;

    /**
     * Creates a ReputationDisplayDto from a User entity.
     *
     * @param user The user whose reputation to display
     * @return ReputationDisplayDto with sanitized reputation information
     */
    public static ReputationDisplayDto fromUser(User user) {
        ReputationDisplayDto dto = new ReputationDisplayDto();

        // Trust level
        UserReputationScore.TrustLevel trustLevel = user.getTrustLevel();
        dto.setTrustLevel(trustLevel.name());
        dto.setTrustLevelDescription(getTrustLevelDescription(trustLevel));

        // Overall score (rounded to hide exact values)
        double rawScore = user.getReputationOverall();
        dto.setOverallScore((int) Math.round(rawScore / 10) * 10); // Round to nearest 10

        // Individual scores (rounded)
        if (user.getReputationScore() != null) {
            UserReputationScore rep = user.getReputationScore();
            dto.setResponseQuality((int) Math.round(rep.getResponseQuality() / 10) * 10);
            dto.setRespectScore((int) Math.round(rep.getRespectScore() / 10) * 10);
            dto.setAuthenticityScore((int) Math.round(rep.getAuthenticityScore() / 10) * 10);
            dto.setInvestmentScore((int) Math.round(rep.getInvestmentScore() / 10) * 10);
            dto.setDatesCompleted(rep.getDatesCompleted());
        }

        // Feedback count
        dto.setPositiveFeedbackCount(user.getPositiveFeedbackCount());

        // Video verification
        dto.setVideoVerified(user.isVideoVerified());

        // Member since
        if (user.getDates() != null && user.getDates().getCreationDate() != null) {
            Date creationDate = user.getDates().getCreationDate();
            LocalDateTime ldt = LocalDateTime.ofInstant(creationDate.toInstant(), ZoneId.systemDefault());
            dto.setMemberSince(ldt.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        } else {
            dto.setMemberSince("Recently");
        }

        return dto;
    }

    /**
     * Get a human-readable description for each trust level.
     *
     * @param level The trust level
     * @return Description text
     */
    public static String getTrustLevelDescription(UserReputationScore.TrustLevel level) {
        return switch (level) {
            case NEW_MEMBER -> "New to AURA - building their reputation";
            case VERIFIED -> "Verified member with basic verification";
            case TRUSTED -> "Trusted member with positive history";
            case HIGHLY_TRUSTED -> "Exemplary member with excellent track record";
            case UNDER_REVIEW -> "Account under review";
            case PROBATION -> "On probation while rebuilding trust";
            case RESTRICTED -> "Limited features due to policy violations";
        };
    }

    /**
     * Get an icon class for the trust level.
     *
     * @param level The trust level
     * @return Font Awesome icon class
     */
    public static String getTrustLevelIcon(UserReputationScore.TrustLevel level) {
        return switch (level) {
            case HIGHLY_TRUSTED -> "fas fa-shield-alt";
            case TRUSTED -> "fas fa-check-circle";
            case VERIFIED -> "fas fa-user-check";
            case NEW_MEMBER -> "fas fa-user";
            case UNDER_REVIEW -> "fas fa-clock";
            case PROBATION -> "fas fa-user-clock";
            case RESTRICTED -> "fas fa-exclamation-triangle";
        };
    }

    /**
     * Get a CSS class for the trust level badge color.
     *
     * @param level The trust level
     * @return CSS class name
     */
    public static String getTrustLevelBadgeClass(UserReputationScore.TrustLevel level) {
        return switch (level) {
            case HIGHLY_TRUSTED -> "aura-trait-positive";
            case TRUSTED -> "aura-trait-positive";
            case VERIFIED -> "";
            case NEW_MEMBER -> "";
            case UNDER_REVIEW -> "aura-trait-warning";
            case PROBATION -> "aura-trait-warning";
            case RESTRICTED -> "aura-trait-negative";
        };
    }
}
