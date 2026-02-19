package com.nonononoki.alovoa.entity;

import java.util.Date;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(indexes = {
    @Index(name = "idx_profile_user", columnList = "user_id")
})
public class UserAssessmentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column
    private Double opennessScore;

    @Column
    private Double conscientiousnessScore;

    @Column
    private Double extraversionScore;

    @Column
    private Double agreeablenessScore;

    @Column
    private Double neuroticismScore;

    @Column
    private Double emotionalStabilityScore;

    @Column
    private Double attachmentAnxietyScore;

    @Column
    private Double attachmentAvoidanceScore;

    @Column
    @Enumerated(EnumType.STRING)
    private AttachmentStyle attachmentStyle;

    @Column
    private Double valuesProgressiveScore;

    @Column
    private Double valuesEgalitarianScore;

    @Column
    private Double lifestyleSocialScore;

    @Column
    private Double lifestyleHealthScore;

    @Column
    private Double lifestyleWorkLifeScore;

    @Column
    private Double lifestyleFinanceScore;

    @Column
    private Integer dealbreakerFlags;

    @Column
    private Integer bigFiveQuestionsAnswered;

    @Column
    private Integer attachmentQuestionsAnswered;

    @Column
    private Integer valuesQuestionsAnswered;

    @Column
    private Integer lifestyleQuestionsAnswered;

    @Column
    private Integer dealbreakerQuestionsAnswered;

    @Column
    private Boolean bigFiveComplete;

    @Column
    private Boolean attachmentComplete;

    @Column
    private Boolean valuesComplete;

    @Column
    private Boolean dealbreakerComplete;

    @Column
    private Boolean lifestyleComplete;

    @Column
    private Boolean profileComplete;

    // === Growth-Context Profile (Traits vs State) ===

    @Column(length = 255)
    private String purposeStatement;

    @Column(length = 80)
    private String currentChapter;

    @Lob
    @Column(columnDefinition = "text")
    private String valuesHierarchyJson;

    @Lob
    @Column(columnDefinition = "text")
    private String valueTradeoffsJson;

    @Lob
    @Column(columnDefinition = "text")
    private String growthArchetypesJson;

    @Lob
    @Column(columnDefinition = "text")
    private String pacePreferencesJson;

    @Lob
    @Column(columnDefinition = "text")
    private String relationshipIntentionsJson;

    @Lob
    @Column(columnDefinition = "text")
    private String boundariesJson;

    @Lob
    @Column(columnDefinition = "text")
    private String shadowPatternsJson;

    @Lob
    @Column(columnDefinition = "text")
    private String stateContextJson;

    @Column(nullable = false)
    private Date lastUpdated;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        lastUpdated = new Date();
        checkCompletion();
    }

    private void checkCompletion() {
        // Minimum thresholds for category completion
        // Big Five: 25 questions (5 traits x 5 questions minimum)
        // Attachment: 4 questions
        // Values/Dealbreaker/Lifestyle: 5 questions minimum
        bigFiveComplete = bigFiveQuestionsAnswered != null && bigFiveQuestionsAnswered >= 25;
        attachmentComplete = attachmentQuestionsAnswered != null && attachmentQuestionsAnswered >= 4;
        valuesComplete = valuesQuestionsAnswered != null && valuesQuestionsAnswered >= 5;
        dealbreakerComplete = dealbreakerQuestionsAnswered != null && dealbreakerQuestionsAnswered >= 5;
        lifestyleComplete = lifestyleQuestionsAnswered != null && lifestyleQuestionsAnswered >= 5;

        profileComplete = Boolean.TRUE.equals(bigFiveComplete) &&
                          Boolean.TRUE.equals(attachmentComplete) &&
                          Boolean.TRUE.equals(valuesComplete) &&
                          Boolean.TRUE.equals(dealbreakerComplete);
    }

    public enum AttachmentStyle {
        SECURE,
        ANXIOUS_PREOCCUPIED,
        DISMISSIVE_AVOIDANT,
        FEARFUL_AVOIDANT
    }
}
