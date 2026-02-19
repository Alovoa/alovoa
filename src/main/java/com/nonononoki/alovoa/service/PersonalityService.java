package com.nonononoki.alovoa.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserPersonalityProfile;
import com.nonononoki.alovoa.entity.user.UserPersonalityProfile.AttachmentStyle;
import com.nonononoki.alovoa.model.PersonalityAssessmentDto;
import com.nonononoki.alovoa.repo.UserPersonalityProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class PersonalityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersonalityService.class);

    @Value("${app.aura.ai-service.url:http://localhost:8002}")
    private String aiServiceUrl;

    @Value("${app.aura.backend.java-ai.enabled:true}")
    private boolean javaAiEnabled;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserPersonalityProfileRepository personalityRepo;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ReputationService reputationService;

    public Map<String, Object> getAssessmentQuestions() {
        // Return the Big Five assessment questions
        List<Map<String, Object>> questions = new ArrayList<>();

        // Openness questions
        questions.add(createQuestion("O1", "I have a vivid imagination", "openness"));
        questions.add(createQuestion("O2", "I am interested in abstract ideas", "openness"));
        questions.add(createQuestion("O3", "I enjoy trying new things", "openness"));
        questions.add(createQuestion("O4", "I appreciate art and beauty", "openness"));
        questions.add(createQuestion("O5", "I am curious about many different things", "openness"));

        // Conscientiousness questions
        questions.add(createQuestion("C1", "I am always prepared", "conscientiousness"));
        questions.add(createQuestion("C2", "I pay attention to details", "conscientiousness"));
        questions.add(createQuestion("C3", "I follow a schedule", "conscientiousness"));
        questions.add(createQuestion("C4", "I like order and organization", "conscientiousness"));
        questions.add(createQuestion("C5", "I complete tasks thoroughly", "conscientiousness"));

        // Extraversion questions
        questions.add(createQuestion("E1", "I am the life of the party", "extraversion"));
        questions.add(createQuestion("E2", "I feel comfortable around people", "extraversion"));
        questions.add(createQuestion("E3", "I start conversations easily", "extraversion"));
        questions.add(createQuestion("E4", "I enjoy being the center of attention", "extraversion"));
        questions.add(createQuestion("E5", "I have many friends", "extraversion"));

        // Agreeableness questions
        questions.add(createQuestion("A1", "I am interested in people", "agreeableness"));
        questions.add(createQuestion("A2", "I sympathize with others' feelings", "agreeableness"));
        questions.add(createQuestion("A3", "I take time out for others", "agreeableness"));
        questions.add(createQuestion("A4", "I feel others' emotions", "agreeableness"));
        questions.add(createQuestion("A5", "I make people feel at ease", "agreeableness"));

        // Neuroticism questions
        questions.add(createQuestion("N1", "I get stressed out easily", "neuroticism"));
        questions.add(createQuestion("N2", "I worry about things", "neuroticism"));
        questions.add(createQuestion("N3", "I am easily disturbed", "neuroticism"));
        questions.add(createQuestion("N4", "I change my mood a lot", "neuroticism"));
        questions.add(createQuestion("N5", "I get irritated easily", "neuroticism"));

        // Attachment style questions
        questions.add(createQuestion("AT1", "I find it easy to depend on others", "attachment"));
        questions.add(createQuestion("AT2", "I worry about being abandoned", "attachment"));
        questions.add(createQuestion("AT3", "I prefer not to show how I feel deep down", "attachment"));
        questions.add(createQuestion("AT4", "I am comfortable with emotional intimacy", "attachment"));

        return Map.of(
                "questions", questions,
                "scaleMin", 1,
                "scaleMax", 5,
                "scaleLabels", List.of("Strongly Disagree", "Disagree", "Neutral", "Agree", "Strongly Agree"),
                "version", 1
        );
    }

    private Map<String, Object> createQuestion(String id, String text, String category) {
        return Map.of(
                "id", id,
                "text", text,
                "category", category.toUpperCase()
        );
    }

    public Map<String, Object> submitAssessment(PersonalityAssessmentDto assessment) throws Exception {
        User user = authService.getCurrentUser(true);

        UserPersonalityProfile profile = personalityRepo.findByUser(user)
                .orElseGet(() -> {
                    UserPersonalityProfile newProfile = new UserPersonalityProfile();
                    newProfile.setUser(user);
                    return newProfile;
                });

        // Calculate Big Five scores from answers
        Map<String, List<Integer>> traitAnswers = new HashMap<>();
        traitAnswers.put("openness", new ArrayList<>());
        traitAnswers.put("conscientiousness", new ArrayList<>());
        traitAnswers.put("extraversion", new ArrayList<>());
        traitAnswers.put("agreeableness", new ArrayList<>());
        traitAnswers.put("neuroticism", new ArrayList<>());
        traitAnswers.put("attachment", new ArrayList<>());

        for (Map.Entry<String, Integer> answer : assessment.getAnswers().entrySet()) {
            String questionId = answer.getKey();
            String trait = getTraitFromQuestionId(questionId);
            if (trait != null && traitAnswers.containsKey(trait)) {
                traitAnswers.get(trait).add(answer.getValue());
            }
        }

        // Calculate average scores and convert to 0-100 scale
        profile.setOpenness(calculateTraitScore(traitAnswers.get("openness")));
        profile.setConscientiousness(calculateTraitScore(traitAnswers.get("conscientiousness")));
        profile.setExtraversion(calculateTraitScore(traitAnswers.get("extraversion")));
        profile.setAgreeableness(calculateTraitScore(traitAnswers.get("agreeableness")));
        profile.setNeuroticism(calculateTraitScore(traitAnswers.get("neuroticism")));

        // Calculate attachment style
        List<Integer> attachmentAnswers = traitAnswers.get("attachment");
        if (attachmentAnswers.size() >= 4) {
            profile.setAttachmentStyle(calculateAttachmentStyle(attachmentAnswers));
            profile.setAttachmentConfidence(0.75); // Base confidence
        }

        // Store raw answers
        profile.setValuesAnswers(objectMapper.writeValueAsString(assessment.getAnswers()));
        profile.setAssessmentCompletedAt(new Date());
        profile.setAssessmentVersion(1);

        personalityRepo.save(profile);

        // Try to generate embeddings via AI service
        try {
            generatePersonalityEmbeddings(user, profile);
        } catch (Exception e) {
            LOGGER.warn("Failed to generate personality embeddings", e);
        }

        // Record profile completion behavior
        if (profile.isComplete()) {
            reputationService.recordBehavior(user,
                    com.nonononoki.alovoa.entity.user.UserBehaviorEvent.BehaviorType.PROFILE_COMPLETE,
                    null, Map.of("type", "personality_assessment"));
        }

        return Map.of(
                "success", true,
                "profile", getProfileSummary(profile)
        );
    }

    private String getTraitFromQuestionId(String questionId) {
        if (questionId.startsWith("O")) return "openness";
        if (questionId.startsWith("C")) return "conscientiousness";
        if (questionId.startsWith("E")) return "extraversion";
        if (questionId.startsWith("AT")) return "attachment"; // Check AT before A
        if (questionId.startsWith("A")) return "agreeableness";
        if (questionId.startsWith("N")) return "neuroticism";
        return null;
    }

    private Double calculateTraitScore(List<Integer> answers) {
        if (answers == null || answers.isEmpty()) return null;
        double avg = answers.stream().mapToInt(Integer::intValue).average().orElse(3.0);
        // Convert from 1-5 scale to 0-100
        return (avg - 1) * 25;
    }

    private AttachmentStyle calculateAttachmentStyle(List<Integer> answers) {
        // Simple attachment style calculation based on 4 questions:
        // AT1: Easy to depend on others (high = secure/anxious, low = avoidant)
        // AT2: Worry about abandonment (high = anxious, low = secure/avoidant)
        // AT3: Prefer not to show feelings (high = avoidant, low = secure/anxious)
        // AT4: Comfortable with intimacy (high = secure, low = avoidant/disorganized)

        int dependence = answers.get(0);
        int abandonmentWorry = answers.get(1);
        int emotionalHiding = answers.get(2);
        int intimacyComfort = answers.get(3);

        // Simple classification
        if (intimacyComfort >= 4 && abandonmentWorry <= 2 && emotionalHiding <= 2) {
            return AttachmentStyle.SECURE;
        } else if (abandonmentWorry >= 4) {
            return AttachmentStyle.ANXIOUS;
        } else if (emotionalHiding >= 4 && dependence <= 2) {
            return AttachmentStyle.AVOIDANT;
        } else if (intimacyComfort <= 2 && abandonmentWorry >= 3) {
            return AttachmentStyle.DISORGANIZED;
        }

        return AttachmentStyle.SECURE; // Default
    }

    private void generatePersonalityEmbeddings(User user, UserPersonalityProfile profile) {
        if (javaAiEnabled) {
            String seed = String.format(
                    Locale.ROOT,
                    "%d:%.2f:%.2f:%.2f:%.2f:%.2f",
                    user.getId(),
                    profile.getOpenness() == null ? 0.0 : profile.getOpenness(),
                    profile.getConscientiousness() == null ? 0.0 : profile.getConscientiousness(),
                    profile.getExtraversion() == null ? 0.0 : profile.getExtraversion(),
                    profile.getAgreeableness() == null ? 0.0 : profile.getAgreeableness(),
                    profile.getNeuroticism() == null ? 0.0 : profile.getNeuroticism()
            );
            profile.setPersonalityEmbeddingId("java_local_" + Integer.toHexString(seed.hashCode()));
            personalityRepo.save(profile);
            return;
        }

        try {
            String url = aiServiceUrl + "/embeddings/personality";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = Map.of(
                    "user_id", user.getId(),
                    "openness", profile.getOpenness(),
                    "conscientiousness", profile.getConscientiousness(),
                    "extraversion", profile.getExtraversion(),
                    "agreeableness", profile.getAgreeableness(),
                    "neuroticism", profile.getNeuroticism()
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getBody() != null) {
                Map<String, Object> result = response.getBody();
                if (result.containsKey("embedding_id")) {
                    profile.setPersonalityEmbeddingId((String) result.get("embedding_id"));
                    personalityRepo.save(profile);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to call AI embedding service", e);
        }
    }

    public Map<String, Object> getPersonalityResults() throws Exception {
        User user = authService.getCurrentUser(true);

        UserPersonalityProfile profile = personalityRepo.findByUser(user)
                .orElse(null);

        if (profile == null || !profile.isComplete()) {
            return Map.of(
                    "hasResults", false,
                    "message", "Please complete the personality assessment first"
            );
        }

        return Map.of(
                "hasResults", true,
                "profile", getProfileSummary(profile)
        );
    }

    private Map<String, Object> getProfileSummary(UserPersonalityProfile profile) {
        Map<String, Object> summary = new HashMap<>();

        summary.put("bigFive", Map.of(
                "openness", profile.getOpenness(),
                "conscientiousness", profile.getConscientiousness(),
                "extraversion", profile.getExtraversion(),
                "agreeableness", profile.getAgreeableness(),
                "neuroticism", profile.getNeuroticism()
        ));

        if (profile.getAttachmentStyle() != null) {
            summary.put("attachmentStyle", profile.getAttachmentStyle().name());
            summary.put("attachmentConfidence", profile.getAttachmentConfidence());
        }

        summary.put("completedAt", profile.getAssessmentCompletedAt());
        summary.put("version", profile.getAssessmentVersion());

        return summary;
    }

    public Map<String, Object> retakeAssessment() throws Exception {
        User user = authService.getCurrentUser(true);

        UserPersonalityProfile profile = personalityRepo.findByUser(user)
                .orElse(null);

        if (profile != null) {
            // Reset the profile
            profile.setOpenness(null);
            profile.setConscientiousness(null);
            profile.setExtraversion(null);
            profile.setAgreeableness(null);
            profile.setNeuroticism(null);
            profile.setAttachmentStyle(null);
            profile.setValuesAnswers(null);
            profile.setAssessmentCompletedAt(null);
            profile.setPersonalityEmbeddingId(null);
            personalityRepo.save(profile);
        }

        return Map.of("success", true);
    }

    /**
     * Generate matching insights based on personality profile.
     * These insights help users understand how their personality affects matching.
     */
    public List<String> getMatchingInsights(UserPersonalityProfile profile) {
        List<String> insights = new ArrayList<>();

        if (profile == null || !profile.isComplete()) {
            return insights;
        }

        // Extraversion insight
        if (profile.getExtraversion() != null) {
            if (profile.getExtraversion() >= 70) {
                insights.add("Your outgoing nature means you'll thrive with partners who enjoy social activities and spontaneous adventures.");
            } else if (profile.getExtraversion() <= 30) {
                insights.add("You value deep one-on-one connections. Look for partners who appreciate meaningful conversations over large gatherings.");
            }
        }

        // Openness insight
        if (profile.getOpenness() != null) {
            if (profile.getOpenness() >= 70) {
                insights.add("Your curiosity and creativity will attract partners who share your love for new experiences and ideas.");
            } else if (profile.getOpenness() <= 30) {
                insights.add("You appreciate stability and tradition. Partners with similar values will share your grounded approach to life.");
            }
        }

        // Agreeableness insight
        if (profile.getAgreeableness() != null) {
            if (profile.getAgreeableness() >= 70) {
                insights.add("Your caring and empathetic nature makes you a supportive partner. Look for someone who values emotional connection.");
            }
        }

        // Neuroticism insight
        if (profile.getNeuroticism() != null) {
            if (profile.getNeuroticism() >= 60) {
                insights.add("You experience emotions deeply. A patient, understanding partner can help create a secure relationship foundation.");
            } else if (profile.getNeuroticism() <= 30) {
                insights.add("Your emotional stability is an asset. You bring calm to relationships and can help partners feel secure.");
            }
        }

        // Attachment style insight
        if (profile.getAttachmentStyle() != null) {
            switch (profile.getAttachmentStyle()) {
                case SECURE:
                    insights.add("Your secure attachment style helps create stable, trusting relationships with all partner types.");
                    break;
                case ANXIOUS:
                    insights.add("Partners with secure or patient attachment styles can help you feel more confident in relationships.");
                    break;
                case AVOIDANT:
                    insights.add("Consider partners who respect your need for independence while gently encouraging emotional openness.");
                    break;
                case DISORGANIZED:
                    insights.add("A secure, consistent partner can help you build trust and feel safe in the relationship.");
                    break;
            }
        }

        return insights;
    }
}
