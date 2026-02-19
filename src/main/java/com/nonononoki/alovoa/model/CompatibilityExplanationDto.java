package com.nonononoki.alovoa.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * DTO for displaying compatibility explanation to users.
 * Shows why two users are compatible and where they might face challenges.
 *
 * Aligned with OKCupid-style Marriage Machine features in compatibility-explanation.html
 */
@Getter
@Setter
public class CompatibilityExplanationDto {

    // === Basic Info ===

    /**
     * Name of the match (for personalized display)
     */
    private String matchName;

    // === Core Scores ===

    /**
     * Overall compatibility score (0-100)
     * @deprecated Use matchCategoryLabel for user-facing display instead of numerical percentages.
     */
    @Deprecated
    private Double overallScore;

    /**
     * OKCupid-style match percentage using geometric mean formula:
     * sqrt(yourSatisfaction * theirSatisfaction) * 100
     * @deprecated Use matchCategoryLabel for user-facing display instead of numerical percentages.
     */
    @Deprecated
    private Double matchPercentage;

    /**
     * Non-deprecated internal accessor for legacy API payload compatibility.
     */
    public Double getOverallScoreValue() {
        return overallScore;
    }

    /**
     * Non-deprecated internal accessor for legacy API payload compatibility.
     */
    public Double getMatchPercentageValue() {
        return matchPercentage;
    }

    /**
     * Human-readable match category label for user-facing display.
     * Values: "Exceptional Match", "Strong Match", "Good Match", "Fair Match", "Exploring Match"
     */
    private String matchCategoryLabel;

    /**
     * Dimension-level labels (non-numerical).
     * Maps dimension names to human-readable labels.
     * e.g., {"Values": "Excellent", "Lifestyle": "Good", "Personality": "Fair"}
     */
    private Map<String, String> dimensionLabels;

    /**
     * Enemy score - measures fundamental incompatibilities (0-100)
     * Higher = more incompatible (like OKCupid's enemy percentage)
     */
    private Double enemyScore;

    // === Bidirectional Scores (OKCupid feature) ===

    /**
     * How much they match what YOU want (0.0-1.0)
     * Displayed as "You → Them" score
     */
    private Double yourSatisfaction;

    /**
     * How much YOU match what THEY want (0.0-1.0)
     * Displayed as "Them → You" score
     */
    private Double theirSatisfaction;

    // === Dealbreaker Detection ===

    /**
     * Whether a mandatory question conflict exists
     */
    private Boolean hasDealbreaker;

    /**
     * List of dealbreaker conflicts with details
     */
    private List<DealbreakderDetail> dealbreakers;

    /**
     * Same as hasDealbreaker (alias for frontend compatibility)
     */
    private Boolean mandatoryConflicts;

    // === Category Breakdown ===

    /**
     * Marriage Machine category breakdown (0-100 per category)
     * Keys: BIG_FIVE, ATTACHMENT, VALUES, LIFESTYLE, SEX, COMMUNICATION, RELATIONSHIP
     */
    private Map<String, Double> categoryBreakdown;

    /**
     * Legacy dimension scores (for backward compatibility)
     * Keys: "values", "lifestyle", "personality", "attraction", "circumstantial", "growth"
     */
    private Map<String, Double> dimensionScores;

    // === Insights ===

    /**
     * List of top compatibility strengths
     * e.g., ["Similar communication styles", "Shared values around family"]
     */
    private List<String> topCompatibilities;

    /**
     * List of potential challenges or areas requiring work
     * e.g., ["Different sleep schedules", "Varying social energy levels"]
     */
    private List<String> potentialChallenges;

    /**
     * Areas to discuss (alias for potentialChallenges for frontend compatibility)
     */
    private List<String> areasToDiscuss;

    /**
     * Match insight object with structured compatibility info
     */
    private MatchInsight matchInsight;

    // === Question Analysis ===

    /**
     * Number of questions both users answered (for reliability indicator)
     */
    private Integer questionsCompared;

    /**
     * Question-by-question match details (expandable section)
     */
    private List<QuestionMatch> questionMatches;

    // === Textual Content ===

    /**
     * Brief summary of compatibility
     * e.g., "You two have strong alignment in values and personality traits..."
     */
    private String summary;

    /**
     * Detailed explanation from AI service (optional)
     * Contains structured breakdown of compatibility factors
     */
    private Map<String, Object> detailedExplanation;

    // === Nested DTOs ===

    @Getter
    @Setter
    public static class DealbreakderDetail {
        private String question;
        private String category;
        private String yourAnswer;
        private String theirAnswer;
    }

    @Getter
    @Setter
    public static class MatchInsight {
        private List<String> topAreas;
        private List<String> areasToDiscuss;
        private String summary;
    }

    @Getter
    @Setter
    public static class QuestionMatch {
        private String questionText;
        private String yourAnswer;
        private String theirAnswer;
        private String yourImportance;
        private String theirImportance;
        private Boolean isMatch;
        private Boolean isPartialMatch;
        private String importance;  // Overall importance level
    }
}
