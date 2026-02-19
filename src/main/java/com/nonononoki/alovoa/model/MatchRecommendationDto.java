package com.nonononoki.alovoa.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class MatchRecommendationDto {
    private Long userId;
    private String userUuid;
    private Double compatibilityScore;
    private Double enemyScore;  // Incompatibility percentage (OKCupid 2016 parity)
    private List<String> conversationStarters;
    private List<String> sharedInterests;
    private String compatibilitySummary;

    // Privacy-safe location info (centroid-based, NOT GPS)
    private Integer travelTimeMinutes;      // ~X min from your areas
    private String travelTimeDisplay;        // Display string like "~15 min"
    private boolean hasOverlappingAreas;     // Do users share any declared areas?
    private List<String> overlappingAreas;   // Shared area names (e.g., ["Downtown DC"])

    // === OKCupid-Style Match Percentage (Marriage Machine Feature) ===

    /**
     * OkCupid-style match percentage using geometric mean formula:
     * sqrt(your_satisfaction * their_satisfaction) * 100
     *
     * This is THE KEY metric for marriage machines - not just compatibility,
     * but mutual satisfaction based on importance-weighted answers.
     *
     * @deprecated Use matchCategory for user-facing display instead of numerical percentages.
     *             Kept for internal calculations.
     */
    @Deprecated
    private Double matchPercentage;

    /**
     * Non-deprecated internal accessor for API adapters that still need the numeric value.
     */
    public Double getMatchPercentageValue() {
        return matchPercentage;
    }

    /**
     * Human-readable match category label for user-facing display.
     * Replaces numerical percentages to prevent optimization/gaming behavior.
     *
     * Values: "Exceptional Match", "Strong Match", "Good Match", "Fair Match", "Exploring Match"
     */
    private String matchCategory;

    /**
     * Dimension-level labels (non-numerical).
     * Maps dimension names to human-readable labels.
     * e.g., {"Values": "Excellent", "Lifestyle": "Good", "Personality": "Fair"}
     */
    private Map<String, String> categoryLabels;

    /**
     * Category-level breakdown of match percentage.
     * Keys: BIG_FIVE, ATTACHMENT, VALUES, LIFESTYLE, DEALBREAKER
     * Values: 0-100 percentage for each category
     *
     * Shows users WHERE they're compatible (e.g., "95% on values, 78% on lifestyle")
     */
    private Map<String, Double> categoryBreakdown;

    /**
     * Number of questions both users have answered.
     * Higher = more reliable match percentage.
     * Show "Based on X shared questions" in UI.
     */
    private Integer commonQuestionsCount;

    /**
     * Flag indicating a dealbreaker/mandatory conflict exists.
     * If true, match percentage is capped at 10% regardless of other scores.
     * Show clear warning in UI: "⚠️ Potential dealbreaker detected"
     */
    private Boolean hasMandatoryConflict;

    /**
     * Brief explanation of the top compatibility areas.
     * e.g., "You both value honesty highly and have similar attachment styles"
     */
    private String matchInsight;

    /**
     * Top 3 areas of strong compatibility (for UI highlighting).
     * e.g., ["Values alignment: 94%", "Communication style: 89%", "Life goals: 87%"]
     */
    private List<String> topCompatibilityAreas;

    /**
     * Areas that need discussion (moderate mismatches, not dealbreakers).
     * e.g., ["Different social energy levels", "Work-life balance preferences"]
     */
    private List<String> areasToDiscuss;

    /**
     * Intro message from match window (if any).
     * This is the pre-match "personality leads" message.
     */
    private String introMessage;

    /**
     * Whether this user has sent an intro message in the match window.
     */
    private Boolean hasIntroMessage;
}
