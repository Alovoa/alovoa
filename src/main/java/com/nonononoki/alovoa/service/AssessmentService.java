package com.nonononoki.alovoa.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.entity.AssessmentQuestion;
import com.nonononoki.alovoa.entity.AssessmentQuestion.QuestionCategory;
import com.nonononoki.alovoa.entity.AssessmentQuestion.ResponseScale;
import com.nonononoki.alovoa.entity.AssessmentQuestion.Severity;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.UserAssessmentProfile;
import com.nonononoki.alovoa.entity.UserAssessmentProfile.AttachmentStyle;
import com.nonononoki.alovoa.entity.UserAssessmentResponse;
import com.nonononoki.alovoa.model.AssessmentResponseDto;
import com.nonononoki.alovoa.repo.AssessmentQuestionRepository;
import com.nonononoki.alovoa.repo.UserAssessmentProfileRepository;
import com.nonononoki.alovoa.repo.UserAssessmentResponseRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AssessmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssessmentService.class);

    private static final String QUESTION_BANK_PATH = "data/aura-comprehensive-questions.json";

    // OKCupid-style importance weights
    private static final Map<String, Double> IMPORTANCE_WEIGHTS = Map.of(
            "irrelevant", 0.0,
            "a_little", 1.0,
            "somewhat", 10.0,
            "very", 50.0,
            "mandatory", 250.0
    );

    // Big Five domain mappings
    private static final Map<String, String> DOMAIN_MAP = Map.of(
            "OPENNESS", "O",
            "CONSCIENTIOUSNESS", "C",
            "EXTRAVERSION", "E",
            "AGREEABLENESS", "A",
            "NEUROTICISM", "N"
    );

    @Value("${aura.assessment.auto-load:true}")
    private boolean autoLoadQuestions;

    @Autowired
    private AuthService authService;

    @Autowired
    private AssessmentQuestionRepository questionRepo;

    @Autowired
    private UserAssessmentResponseRepository responseRepo;

    @Autowired
    private UserAssessmentProfileRepository profileRepo;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReputationService reputationService;

    @PostConstruct
    public void init() {
        if (autoLoadQuestions) {
            try {
                loadComprehensiveQuestions();
            } catch (Exception e) {
                LOGGER.error("Failed to load assessment questions from JSON", e);
            }
        }
    }

    /**
     * Load questions from the comprehensive flat-array format.
     * This is the new AURA question bank with 4,127 questions.
     */
    @Transactional
    public void loadComprehensiveQuestions() throws Exception {
        ClassPathResource resource = new ClassPathResource(QUESTION_BANK_PATH);
        if (!resource.exists()) {
            LOGGER.warn("Comprehensive question bank not found: {}", QUESTION_BANK_PATH);
            return;
        }

        try (InputStream is = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            JsonNode questions = root.get("questions");

            if (questions == null || !questions.isArray()) {
                LOGGER.warn("No questions array found in question bank");
                return;
            }

            int loaded = 0;
            int skipped = 0;

            for (JsonNode q : questions) {
                String externalId = q.has("id") ? q.get("id").asText() : null;
                if (externalId == null) continue;

                if (questionRepo.existsByExternalId(externalId)) {
                    // Update coreQuestion flag if needed
                    if (q.has("coreQuestion") && q.get("coreQuestion").asBoolean()) {
                        questionRepo.findByExternalId(externalId).ifPresent(existing -> {
                            if (!Boolean.TRUE.equals(existing.getCoreQuestion())) {
                                existing.setCoreQuestion(true);
                                questionRepo.save(existing);
                            }
                        });
                    }
                    skipped++;
                    continue;
                }

                AssessmentQuestion question = new AssessmentQuestion();
                question.setExternalId(externalId);
                question.setText(q.has("text") ? q.get("text").asText() :
                               (q.has("question") ? q.get("question").asText() : ""));

                // Set category from string
                String categoryStr = q.has("category") ? q.get("category").asText() : null;
                question.setSubcategory(categoryStr); // Store original category as subcategory

                // Map to QuestionCategory enum
                QuestionCategory category = mapStringToCategory(categoryStr);
                question.setCategory(category);

                // Set response scale based on question type
                ResponseScale scale = determineResponseScale(q);
                question.setResponseScale(scale);

                // Set optional fields
                if (q.has("subcategory")) {
                    question.setSubcategory(q.get("subcategory").asText());
                }
                if (q.has("domain")) {
                    question.setDomain(q.get("domain").asText());
                }
                if (q.has("facet")) {
                    question.setFacet(q.get("facet").asInt());
                }
                if (q.has("keyed")) {
                    question.setKeyed(q.get("keyed").asText());
                }
                if (q.has("dimension")) {
                    question.setDimension(q.get("dimension").asText());
                }
                if (q.has("coreQuestion")) {
                    question.setCoreQuestion(q.get("coreQuestion").asBoolean());
                }

                // Store options as JSON
                if (q.has("options")) {
                    question.setOptions(objectMapper.writeValueAsString(q.get("options")));
                }

                // Get suggested importance from metadata
                if (q.has("metadata") && q.get("metadata").has("suggested_importance")) {
                    question.setSuggestedImportance(q.get("metadata").get("suggested_importance").asText());
                }

                question.setDisplayOrder(loaded);
                question.setActive(true);

                questionRepo.save(question);
                loaded++;
            }

            LOGGER.info("Loaded {} questions, skipped {} existing (total: {})",
                       loaded, skipped, loaded + skipped);
        }
    }

    private QuestionCategory mapStringToCategory(String categoryStr) {
        if (categoryStr == null) return QuestionCategory.VALUES;

        return switch (categoryStr.toLowerCase()) {
            case "attachment_emotional" -> QuestionCategory.ATTACHMENT;
            case "dealbreakers_safety" -> QuestionCategory.DEALBREAKER;
            case "personality_temperament" -> QuestionCategory.BIG_FIVE;
            case "values_politics", "relationship_dynamics", "family_future",
                 "hypotheticals_scenarios", "location_specific" -> QuestionCategory.VALUES;
            case "lifestyle_compatibility" -> QuestionCategory.LIFESTYLE;
            case "sex_intimacy" -> QuestionCategory.LIFESTYLE;
            default -> QuestionCategory.VALUES;
        };
    }

    private ResponseScale determineResponseScale(JsonNode q) {
        if (q.has("type")) {
            String type = q.get("type").asText();
            if ("binary".equals(type)) return ResponseScale.BINARY;
            if ("free_text".equals(type) || "open_ended".equals(type)) return ResponseScale.FREE_TEXT;
        }

        if (q.has("options") && q.get("options").isArray()) {
            int optionCount = q.get("options").size();
            if (optionCount == 2) return ResponseScale.BINARY;
            if (optionCount >= 5) return ResponseScale.LIKERT_5;
        }

        return ResponseScale.AGREEMENT_5;
    }

    @Transactional
    public void loadQuestionsFromJson() throws Exception {
        ClassPathResource resource = new ClassPathResource(QUESTION_BANK_PATH);
        if (!resource.exists()) {
            LOGGER.warn("Question bank file not found: {}", QUESTION_BANK_PATH);
            return;
        }

        try (InputStream is = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            JsonNode questions = root.get("questions");

            int displayOrder = 0;

            // Load Big Five questions
            displayOrder = loadBigFiveQuestions(questions.get("BIG_FIVE"), displayOrder);

            // Load Attachment questions
            displayOrder = loadAttachmentQuestions(questions.get("ATTACHMENT"), displayOrder);

            // Load Dealbreaker questions
            displayOrder = loadDealbreakerQuestions(questions.get("DEALBREAKER"), displayOrder);

            // Load Values questions
            displayOrder = loadValuesQuestions(questions.get("VALUES"), displayOrder);

            // Load Lifestyle questions
            displayOrder = loadLifestyleQuestions(questions.get("LIFESTYLE"), displayOrder);

            // Load Red Flag questions
            loadRedFlagQuestions(questions.get("RED_FLAG"), displayOrder);

            LOGGER.info("Successfully loaded assessment questions from JSON");
        }
    }

    private int loadBigFiveQuestions(JsonNode bigFive, int displayOrder) {
        if (bigFive == null) return displayOrder;

        String[] domains = {"OPENNESS", "CONSCIENTIOUSNESS", "EXTRAVERSION", "AGREEABLENESS", "NEUROTICISM"};

        for (String domainName : domains) {
            JsonNode domain = bigFive.get(domainName);
            if (domain == null) continue;

            JsonNode questionList = domain.get("questions");
            if (questionList == null || !questionList.isArray()) continue;

            for (JsonNode q : questionList) {
                String externalId = q.get("id").asText();
                if (questionRepo.existsByExternalId(externalId)) continue;

                AssessmentQuestion question = new AssessmentQuestion();
                question.setExternalId(externalId);
                question.setText(q.get("text").asText());
                question.setCategory(QuestionCategory.BIG_FIVE);
                question.setResponseScale(ResponseScale.LIKERT_5);
                question.setDomain(DOMAIN_MAP.get(domainName));
                question.setFacet(q.get("facet").asInt());
                question.setKeyed(q.get("keyed").asText());
                question.setDisplayOrder(displayOrder++);
                question.setActive(true);

                questionRepo.save(question);
            }
        }

        return displayOrder;
    }

    private int loadAttachmentQuestions(JsonNode attachment, int displayOrder) {
        if (attachment == null) return displayOrder;

        JsonNode questionList = attachment.get("questions");
        if (questionList == null || !questionList.isArray()) return displayOrder;

        for (JsonNode q : questionList) {
            String externalId = q.get("id").asText();
            if (questionRepo.existsByExternalId(externalId)) continue;

            AssessmentQuestion question = new AssessmentQuestion();
            question.setExternalId(externalId);
            question.setText(q.get("text").asText());
            question.setCategory(QuestionCategory.ATTACHMENT);
            question.setResponseScale(ResponseScale.AGREEMENT_5);
            question.setDimension(q.has("dimension") ? q.get("dimension").asText() : null);
            question.setKeyed(q.has("keyed") ? q.get("keyed").asText() : "plus");
            question.setDisplayOrder(displayOrder++);
            question.setActive(true);

            questionRepo.save(question);
        }

        return displayOrder;
    }

    private int loadDealbreakerQuestions(JsonNode dealbreaker, int displayOrder) {
        if (dealbreaker == null) return displayOrder;

        JsonNode questionList = dealbreaker.get("questions");
        if (questionList == null || !questionList.isArray()) return displayOrder;

        for (JsonNode q : questionList) {
            String externalId = q.get("id").asText();
            if (questionRepo.existsByExternalId(externalId)) continue;

            AssessmentQuestion question = new AssessmentQuestion();
            question.setExternalId(externalId);
            question.setText(q.get("text").asText());
            question.setCategory(QuestionCategory.DEALBREAKER);
            question.setResponseScale(ResponseScale.BINARY);
            question.setSubcategory(q.has("subcategory") ? q.get("subcategory").asText() : null);
            question.setRedFlagValue(q.has("redFlagIf") ? q.get("redFlagIf").asInt() : null);
            question.setSeverity(q.has("severity") ? Severity.valueOf(q.get("severity").asText()) : Severity.HIGH);
            question.setDisplayOrder(displayOrder++);
            question.setActive(true);

            questionRepo.save(question);
        }

        return displayOrder;
    }

    private int loadValuesQuestions(JsonNode values, int displayOrder) {
        if (values == null) return displayOrder;

        JsonNode subcats = values.get("subcategories");
        if (subcats == null) return displayOrder;

        Iterator<String> subcatNames = subcats.fieldNames();
        while (subcatNames.hasNext()) {
            String subcatName = subcatNames.next();
            JsonNode subcat = subcats.get(subcatName);
            JsonNode questionList = subcat.get("questions");

            if (questionList == null || !questionList.isArray()) continue;

            for (JsonNode q : questionList) {
                String externalId = q.get("id").asText();
                if (questionRepo.existsByExternalId(externalId)) continue;

                AssessmentQuestion question = new AssessmentQuestion();
                question.setExternalId(externalId);
                question.setText(q.get("text").asText());
                question.setCategory(QuestionCategory.VALUES);
                question.setResponseScale(ResponseScale.AGREEMENT_5);
                question.setSubcategory(subcatName);
                question.setDimension(q.has("dimension") ? q.get("dimension").asText() : null);
                question.setDisplayOrder(displayOrder++);
                question.setActive(true);

                questionRepo.save(question);
            }
        }

        return displayOrder;
    }

    private int loadLifestyleQuestions(JsonNode lifestyle, int displayOrder) {
        if (lifestyle == null) return displayOrder;

        JsonNode subcats = lifestyle.get("subcategories");
        if (subcats == null) return displayOrder;

        Iterator<String> subcatNames = subcats.fieldNames();
        while (subcatNames.hasNext()) {
            String subcatName = subcatNames.next();
            JsonNode subcat = subcats.get(subcatName);
            JsonNode questionList = subcat.get("questions");

            if (questionList == null || !questionList.isArray()) continue;

            for (JsonNode q : questionList) {
                String externalId = q.get("id").asText();
                if (questionRepo.existsByExternalId(externalId)) continue;

                AssessmentQuestion question = new AssessmentQuestion();
                question.setExternalId(externalId);
                question.setText(q.get("text").asText());
                question.setCategory(QuestionCategory.LIFESTYLE);
                question.setResponseScale(ResponseScale.FREQUENCY_5);
                question.setSubcategory(subcatName);
                question.setDimension(q.has("dimension") ? q.get("dimension").asText() : null);
                question.setDisplayOrder(displayOrder++);
                question.setActive(true);

                questionRepo.save(question);
            }
        }

        return displayOrder;
    }

    private void loadRedFlagQuestions(JsonNode redFlag, int displayOrder) {
        if (redFlag == null) return;

        JsonNode questionList = redFlag.get("questions");
        if (questionList == null || !questionList.isArray()) return;

        for (JsonNode q : questionList) {
            String externalId = q.get("id").asText();
            if (questionRepo.existsByExternalId(externalId)) continue;

            AssessmentQuestion question = new AssessmentQuestion();
            question.setExternalId(externalId);
            question.setText(q.get("text").asText());
            question.setCategory(QuestionCategory.RED_FLAG);
            question.setResponseScale(ResponseScale.FREE_TEXT);
            question.setSubcategory(q.has("analyzes") ? q.get("analyzes").asText() : null);
            question.setDisplayOrder(displayOrder++);
            question.setActive(true);

            questionRepo.save(question);
        }
    }

    public Map<String, Object> getQuestionsByCategory(String categoryName) throws Exception {
        QuestionCategory category = QuestionCategory.valueOf(categoryName.toUpperCase());
        List<AssessmentQuestion> questions = questionRepo.findActiveQuestionsByCategory(category);

        User user = authService.getCurrentUser(true);
        List<UserAssessmentResponse> responses = responseRepo.findByUserAndCategory(user, category);
        Set<Long> answeredQuestionIds = responses.stream()
                .map(r -> r.getQuestion().getId())
                .collect(Collectors.toSet());

        List<Map<String, Object>> questionList = new ArrayList<>();
        for (AssessmentQuestion q : questions) {
            Map<String, Object> questionData = new HashMap<>();
            questionData.put("id", q.getId());
            questionData.put("externalId", q.getExternalId());
            questionData.put("text", q.getText());
            questionData.put("category", q.getCategory().name()); // Include category for filtering tests
            questionData.put("responseScale", q.getResponseScale().name());
            questionData.put("answered", answeredQuestionIds.contains(q.getId()));

            if (q.getDomain() != null) questionData.put("domain", q.getDomain());
            if (q.getFacet() != null) questionData.put("facet", q.getFacet());
            if (q.getSubcategory() != null) questionData.put("subcategory", q.getSubcategory());
            if (q.getSuggestedImportance() != null) questionData.put("suggestedImportance", q.getSuggestedImportance());

            // Include existing response if answered
            if (answeredQuestionIds.contains(q.getId())) {
                Optional<UserAssessmentResponse> existingResponse = responses.stream()
                        .filter(r -> r.getQuestion().getId().equals(q.getId()))
                        .findFirst();
                existingResponse.ifPresent(r -> {
                    if (r.getNumericResponse() != null) {
                        questionData.put("response", r.getNumericResponse());
                    } else if (r.getTextResponse() != null) {
                        questionData.put("response", r.getTextResponse());
                    }
                });
            }

            questionList.add(questionData);
        }

        // Get subcategories if applicable
        List<String> subcategories = questionRepo.findDistinctSubcategoriesByCategory(category);

        return Map.of(
                "category", categoryName,
                "questions", questionList,
                "subcategories", subcategories,
                "totalQuestions", questions.size(),
                "answeredQuestions", answeredQuestionIds.size()
        );
    }

    public Map<String, Object> getAssessmentProgress() throws Exception {
        User user = authService.getCurrentUser(true);
        UserAssessmentProfile profile = profileRepo.findByUser(user).orElse(null);

        Map<String, Object> progress = new HashMap<>();

        for (QuestionCategory category : QuestionCategory.values()) {
            long totalQuestions = questionRepo.countByCategoryAndActiveTrue(category);
            long answeredQuestions = responseRepo.countByUserAndCategory(user, category);

            Map<String, Object> categoryProgress = new HashMap<>();
            categoryProgress.put("total", totalQuestions);
            categoryProgress.put("answered", answeredQuestions);
            categoryProgress.put("percentage", totalQuestions > 0 ? (answeredQuestions * 100.0 / totalQuestions) : 0);
            categoryProgress.put("complete", profile != null && isCategoryComplete(profile, category));

            progress.put(category.name(), categoryProgress);
        }

        progress.put("profileComplete", profile != null && Boolean.TRUE.equals(profile.getProfileComplete()));

        return progress;
    }

    private boolean isCategoryComplete(UserAssessmentProfile profile, QuestionCategory category) {
        return switch (category) {
            case BIG_FIVE -> Boolean.TRUE.equals(profile.getBigFiveComplete());
            case ATTACHMENT -> Boolean.TRUE.equals(profile.getAttachmentComplete());
            case VALUES -> Boolean.TRUE.equals(profile.getValuesComplete());
            case DEALBREAKER -> Boolean.TRUE.equals(profile.getDealbreakerComplete());
            case LIFESTYLE -> Boolean.TRUE.equals(profile.getLifestyleComplete());
            case RED_FLAG -> true; // Optional category
        };
    }

    @Transactional
    public Map<String, Object> submitResponses(List<AssessmentResponseDto> responses) throws Exception {
        User user = authService.getCurrentUser(true);

        // Get or create user's assessment profile
        UserAssessmentProfile profile = profileRepo.findByUser(user)
                .orElseGet(() -> {
                    UserAssessmentProfile newProfile = new UserAssessmentProfile();
                    newProfile.setUser(user);
                    return newProfile;
                });

        Map<QuestionCategory, Integer> answeredCount = new HashMap<>();
        List<String> savedQuestionIds = new ArrayList<>();

        for (AssessmentResponseDto dto : responses) {
            Optional<AssessmentQuestion> questionOpt = questionRepo.findByExternalId(dto.getQuestionId());
            if (questionOpt.isEmpty()) {
                LOGGER.warn("Question not found: {}", dto.getQuestionId());
                continue;
            }

            AssessmentQuestion question = questionOpt.get();

            // Find or create response
            UserAssessmentResponse response = responseRepo.findByUserAndQuestion(user, question)
                    .orElseGet(() -> {
                        UserAssessmentResponse newResponse = new UserAssessmentResponse();
                        newResponse.setUser(user);
                        newResponse.setQuestion(question);
                        newResponse.setCategory(question.getCategory());
                        return newResponse;
                    });

            // Set the response value
            if (question.getResponseScale() == ResponseScale.FREE_TEXT) {
                response.setTextResponse(dto.getTextResponse());
            } else {
                response.setNumericResponse(dto.getNumericResponse());
            }

            // Set OKCupid-style matching fields (Marriage Machine feature)
            if (dto.getImportance() != null && !dto.getImportance().isEmpty()) {
                response.setImportance(dto.getImportance());
            }
            if (dto.getAcceptableAnswers() != null && !dto.getAcceptableAnswers().isEmpty()) {
                response.setAcceptableAnswers(dto.getAcceptableAnswers());
            }
            if (dto.getExplanation() != null && !dto.getExplanation().isEmpty()) {
                response.setExplanation(dto.getExplanation());
            }
            if (dto.getPublicVisible() != null) {
                response.setPublicVisible(dto.getPublicVisible());
            }

            responseRepo.save(response);
            savedQuestionIds.add(dto.getQuestionId());

            // Track answered counts
            answeredCount.merge(question.getCategory(), 1, Integer::sum);
        }

        // Flush responses to ensure count queries see them
        responseRepo.flush();

        // Update profile question counts
        updateProfileQuestionCounts(user, profile);

        // Save profile to trigger checkCompletion() via @PrePersist/@PreUpdate
        profile = profileRepo.save(profile);

        // Calculate scores if categories are complete (after checkCompletion has run)
        if (Boolean.TRUE.equals(profile.getBigFiveComplete())) {
            calculateBigFiveScores(user, profile);
        }
        if (Boolean.TRUE.equals(profile.getAttachmentComplete())) {
            calculateAttachmentScores(user, profile);
        }
        if (Boolean.TRUE.equals(profile.getValuesComplete())) {
            calculateValuesScores(user, profile);
        }
        if (Boolean.TRUE.equals(profile.getLifestyleComplete())) {
            calculateLifestyleScores(user, profile);
        }

        // Save again if scores were calculated
        profileRepo.save(profile);

        // Record profile completion behavior if complete
        if (Boolean.TRUE.equals(profile.getProfileComplete())) {
            reputationService.recordBehavior(user,
                    com.nonononoki.alovoa.entity.user.UserBehaviorEvent.BehaviorType.PROFILE_COMPLETE,
                    null, Map.of("type", "comprehensive_assessment"));
        }

        return Map.of(
                "success", true,
                "savedQuestions", savedQuestionIds.size(),
                "profileComplete", Boolean.TRUE.equals(profile.getProfileComplete())
        );
    }

    private void updateProfileQuestionCounts(User user, UserAssessmentProfile profile) {
        for (QuestionCategory category : QuestionCategory.values()) {
            long count = responseRepo.countByUserAndCategory(user, category);
            switch (category) {
                case BIG_FIVE -> profile.setBigFiveQuestionsAnswered((int) count);
                case ATTACHMENT -> profile.setAttachmentQuestionsAnswered((int) count);
                case VALUES -> profile.setValuesQuestionsAnswered((int) count);
                case DEALBREAKER -> profile.setDealbreakerQuestionsAnswered((int) count);
                case LIFESTYLE -> profile.setLifestyleQuestionsAnswered((int) count);
            }
        }
    }

    private void calculateBigFiveScores(User user, UserAssessmentProfile profile) {
        // Calculate each domain score using plus/minus keying
        String[] domains = {"O", "C", "E", "A", "N"};

        for (String domain : domains) {
            Double plusAvg = responseRepo.averageScoreByUserAndDomainPlus(user, domain);
            Double minusAvg = responseRepo.averageScoreByUserAndDomainMinus(user, domain);

            // For minus-keyed items, reverse the score (6 - score)
            double plusScore = plusAvg != null ? plusAvg : 3.0;
            double minusScore = minusAvg != null ? (6.0 - minusAvg) : 3.0;

            // Average the plus and reversed-minus scores, then convert to 0-100
            double rawScore = (plusScore + minusScore) / 2.0;
            double normalizedScore = (rawScore - 1) * 25; // Convert 1-5 to 0-100

            switch (domain) {
                case "O" -> profile.setOpennessScore(normalizedScore);
                case "C" -> profile.setConscientiousnessScore(normalizedScore);
                case "E" -> profile.setExtraversionScore(normalizedScore);
                case "A" -> profile.setAgreeablenessScore(normalizedScore);
                case "N" -> {
                    profile.setNeuroticismScore(normalizedScore);
                    // Emotional stability is inverse of neuroticism
                    profile.setEmotionalStabilityScore(100 - normalizedScore);
                }
            }
        }
    }

    private void calculateAttachmentScores(User user, UserAssessmentProfile profile) {
        Double anxietyScore = responseRepo.averageScoreByUserAndDimension(user, "anxiety");
        Double avoidanceScore = responseRepo.averageScoreByUserAndDimension(user, "avoidance");

        if (anxietyScore != null) {
            profile.setAttachmentAnxietyScore((anxietyScore - 1) * 25);
        }
        if (avoidanceScore != null) {
            profile.setAttachmentAvoidanceScore((avoidanceScore - 1) * 25);
        }

        // Determine attachment style based on anxiety and avoidance
        if (anxietyScore != null && avoidanceScore != null) {
            boolean lowAnxiety = anxietyScore < 3.0;
            boolean lowAvoidance = avoidanceScore < 3.0;

            if (lowAnxiety && lowAvoidance) {
                profile.setAttachmentStyle(AttachmentStyle.SECURE);
            } else if (!lowAnxiety && lowAvoidance) {
                profile.setAttachmentStyle(AttachmentStyle.ANXIOUS_PREOCCUPIED);
            } else if (lowAnxiety && !lowAvoidance) {
                profile.setAttachmentStyle(AttachmentStyle.DISMISSIVE_AVOIDANT);
            } else {
                profile.setAttachmentStyle(AttachmentStyle.FEARFUL_AVOIDANT);
            }
        }
    }

    private void calculateValuesScores(User user, UserAssessmentProfile profile) {
        Double progressiveScore = responseRepo.averageScoreByUserAndDimension(user, "progressive");
        Double egalitarianScore = responseRepo.averageScoreByUserAndDimension(user, "egalitarian");

        if (progressiveScore != null) {
            profile.setValuesProgressiveScore((progressiveScore - 1) * 25);
        }
        if (egalitarianScore != null) {
            profile.setValuesEgalitarianScore((egalitarianScore - 1) * 25);
        }
    }

    private void calculateLifestyleScores(User user, UserAssessmentProfile profile) {
        Double socialScore = responseRepo.averageScoreByUserAndDimension(user, "social");
        Double healthScore = responseRepo.averageScoreByUserAndDimension(user, "health");
        Double workLifeScore = responseRepo.averageScoreByUserAndDimension(user, "worklife");
        Double financeScore = responseRepo.averageScoreByUserAndDimension(user, "finance");

        if (socialScore != null) {
            profile.setLifestyleSocialScore((socialScore - 1) * 25);
        }
        if (healthScore != null) {
            profile.setLifestyleHealthScore((healthScore - 1) * 25);
        }
        if (workLifeScore != null) {
            profile.setLifestyleWorkLifeScore((workLifeScore - 1) * 25);
        }
        if (financeScore != null) {
            profile.setLifestyleFinanceScore((financeScore - 1) * 25);
        }
    }

    public Map<String, Object> getAssessmentResults() throws Exception {
        User user = authService.getCurrentUser(true);

        UserAssessmentProfile profile = profileRepo.findByUser(user).orElse(null);
        if (profile == null) {
            return Map.of(
                    "hasResults", false,
                    "message", "Please complete the assessment first"
            );
        }

        Map<String, Object> results = new HashMap<>();
        results.put("hasResults", true);
        results.put("profileComplete", Boolean.TRUE.equals(profile.getProfileComplete()));
        results.put("lastUpdated", profile.getLastUpdated());

        // Big Five results
        if (Boolean.TRUE.equals(profile.getBigFiveComplete())) {
            results.put("bigFive", Map.of(
                    "openness", profile.getOpennessScore(),
                    "conscientiousness", profile.getConscientiousnessScore(),
                    "extraversion", profile.getExtraversionScore(),
                    "agreeableness", profile.getAgreeablenessScore(),
                    "neuroticism", profile.getNeuroticismScore(),
                    "emotionalStability", profile.getEmotionalStabilityScore()
            ));
        }

        // Attachment results
        if (Boolean.TRUE.equals(profile.getAttachmentComplete())) {
            results.put("attachment", Map.of(
                    "style", profile.getAttachmentStyle().name(),
                    "anxietyScore", profile.getAttachmentAnxietyScore(),
                    "avoidanceScore", profile.getAttachmentAvoidanceScore()
            ));
        }

        // Values results
        if (Boolean.TRUE.equals(profile.getValuesComplete())) {
            Map<String, Object> valuesMap = new HashMap<>();
            if (profile.getValuesProgressiveScore() != null) {
                valuesMap.put("progressive", profile.getValuesProgressiveScore());
            }
            if (profile.getValuesEgalitarianScore() != null) {
                valuesMap.put("egalitarian", profile.getValuesEgalitarianScore());
            }
            results.put("values", valuesMap);
        }

        // Lifestyle results
        if (Boolean.TRUE.equals(profile.getLifestyleComplete())) {
            Map<String, Object> lifestyleMap = new HashMap<>();
            if (profile.getLifestyleSocialScore() != null) {
                lifestyleMap.put("social", profile.getLifestyleSocialScore());
            }
            if (profile.getLifestyleHealthScore() != null) {
                lifestyleMap.put("health", profile.getLifestyleHealthScore());
            }
            if (profile.getLifestyleWorkLifeScore() != null) {
                lifestyleMap.put("workLife", profile.getLifestyleWorkLifeScore());
            }
            if (profile.getLifestyleFinanceScore() != null) {
                lifestyleMap.put("finance", profile.getLifestyleFinanceScore());
            }
            results.put("lifestyle", lifestyleMap);
        }

        // Dealbreaker flags
        if (Boolean.TRUE.equals(profile.getDealbreakerComplete()) && profile.getDealbreakerFlags() != null) {
            results.put("dealbreakerFlags", profile.getDealbreakerFlags());
        }

        return results;
    }

    /**
     * Return persisted growth-context profile data (Traits vs State model).
     */
    public Map<String, Object> getGrowthContextProfile() throws Exception {
        User user = authService.getCurrentUser(true);
        UserAssessmentProfile profile = profileRepo.findByUser(user).orElse(null);
        return buildGrowthContextPayload(profile);
    }

    /**
     * Save growth-context profile fields.
     * Accepts both top-level fields and nested traits/state payloads from mobile.
     */
    @Transactional
    public Map<String, Object> saveGrowthContextProfile(Map<String, Object> payload) throws Exception {
        User user = authService.getCurrentUser(true);
        UserAssessmentProfile profile = profileRepo.findByUser(user)
                .orElseGet(() -> {
                    UserAssessmentProfile p = new UserAssessmentProfile();
                    p.setUser(user);
                    return p;
                });

        Map<String, Object> traits = asMap(payload.get("traits"));
        Map<String, Object> state = asMap(payload.get("state"));

        // Allow both nested and flat payloads
        applyGrowthField(profile, "purposeStatement",
                payload.containsKey("purposeStatement") ? payload.get("purposeStatement") : traits.get("purposeStatement"));
        applyGrowthField(profile, "currentChapter",
                payload.containsKey("currentChapter") ? payload.get("currentChapter") : state.get("currentChapter"));

        applyGrowthJsonField(profile, "valuesHierarchy",
                payload.containsKey("valuesHierarchy") ? payload.get("valuesHierarchy") : traits.get("valuesHierarchy"));
        applyGrowthJsonField(profile, "valueTradeoffs",
                payload.containsKey("valueTradeoffs") ? payload.get("valueTradeoffs") : traits.get("valueTradeoffs"));
        applyGrowthJsonField(profile, "growthArchetypes",
                payload.containsKey("growthArchetypes") ? payload.get("growthArchetypes") : traits.get("growthArchetypes"));
        applyGrowthJsonField(profile, "pacePreferences",
                payload.containsKey("pacePreferences") ? payload.get("pacePreferences") : state.get("pacePreferences"));
        applyGrowthJsonField(profile, "relationshipIntentions",
                payload.containsKey("relationshipIntentions") ? payload.get("relationshipIntentions") : traits.get("relationshipIntentions"));
        applyGrowthJsonField(profile, "boundaries",
                payload.containsKey("boundaries") ? payload.get("boundaries") : traits.get("boundaries"));
        applyGrowthJsonField(profile, "shadowPatterns",
                payload.containsKey("shadowPatterns") ? payload.get("shadowPatterns") : traits.get("shadowPatterns"));
        applyGrowthJsonField(profile, "stateContext",
                payload.containsKey("stateContext") ? payload.get("stateContext") : state.get("context"));

        profile = profileRepo.save(profile);
        return buildGrowthContextPayload(profile);
    }

    /**
     * Internal helper used by matching to read growth-context for any user.
     */
    public Map<String, Object> getGrowthContextForUser(User user) {
        UserAssessmentProfile profile = profileRepo.findByUser(user).orElse(null);
        return buildGrowthContextPayload(profile);
    }

    private Map<String, Object> buildGrowthContextPayload(UserAssessmentProfile profile) {
        Map<String, Object> traits = new HashMap<>();
        Map<String, Object> state = new HashMap<>();

        if (profile != null) {
            traits.put("purposeStatement", defaultString(profile.getPurposeStatement()));
            traits.put("valuesHierarchy", fromJsonOrDefault(profile.getValuesHierarchyJson(), List.of()));
            traits.put("valueTradeoffs", fromJsonOrDefault(profile.getValueTradeoffsJson(), List.of()));
            traits.put("growthArchetypes", fromJsonOrDefault(profile.getGrowthArchetypesJson(), Map.of()));
            traits.put("relationshipIntentions", fromJsonOrDefault(profile.getRelationshipIntentionsJson(), Map.of()));
            traits.put("boundaries", fromJsonOrDefault(profile.getBoundariesJson(), List.of()));
            traits.put("shadowPatterns", fromJsonOrDefault(profile.getShadowPatternsJson(), List.of()));

            state.put("currentChapter", defaultString(profile.getCurrentChapter()));
            state.put("pacePreferences", fromJsonOrDefault(profile.getPacePreferencesJson(), Map.of()));
            state.put("context", fromJsonOrDefault(profile.getStateContextJson(), Map.of()));
        } else {
            traits.put("purposeStatement", "");
            traits.put("valuesHierarchy", List.of());
            traits.put("valueTradeoffs", List.of());
            traits.put("growthArchetypes", Map.of());
            traits.put("relationshipIntentions", Map.of());
            traits.put("boundaries", List.of());
            traits.put("shadowPatterns", List.of());

            state.put("currentChapter", "");
            state.put("pacePreferences", Map.of());
            state.put("context", Map.of());
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("traits", traits);
        payload.put("state", state);
        payload.put("purposeStatement", traits.get("purposeStatement"));
        payload.put("currentChapter", state.get("currentChapter"));
        payload.put("valuesHierarchy", traits.get("valuesHierarchy"));
        payload.put("valueTradeoffs", traits.get("valueTradeoffs"));
        payload.put("growthArchetypes", traits.get("growthArchetypes"));
        payload.put("pacePreferences", state.get("pacePreferences"));
        payload.put("relationshipIntentions", traits.get("relationshipIntentions"));
        payload.put("boundaries", traits.get("boundaries"));
        payload.put("shadowPatterns", traits.get("shadowPatterns"));
        payload.put("stateContext", state.get("context"));
        payload.put("updatedAt", profile != null ? profile.getLastUpdated() : null);
        return payload;
    }

    private void applyGrowthField(UserAssessmentProfile profile, String key, Object value) {
        if (value == null) {
            return;
        }
        String normalized = String.valueOf(value).trim();
        switch (key) {
            case "purposeStatement" -> profile.setPurposeStatement(normalized);
            case "currentChapter" -> profile.setCurrentChapter(normalized);
            default -> {
            }
        }
    }

    private void applyGrowthJsonField(UserAssessmentProfile profile, String key, Object value) {
        if (value == null) {
            return;
        }
        String json = toJson(value);
        switch (key) {
            case "valuesHierarchy" -> profile.setValuesHierarchyJson(json);
            case "valueTradeoffs" -> profile.setValueTradeoffsJson(json);
            case "growthArchetypes" -> profile.setGrowthArchetypesJson(json);
            case "pacePreferences" -> profile.setPacePreferencesJson(json);
            case "relationshipIntentions" -> profile.setRelationshipIntentionsJson(json);
            case "boundaries" -> profile.setBoundariesJson(json);
            case "shadowPatterns" -> profile.setShadowPatternsJson(json);
            case "stateContext" -> profile.setStateContextJson(json);
            default -> {
            }
        }
    }

    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> raw) {
            Map<String, Object> map = new HashMap<>();
            raw.forEach((k, v) -> map.put(String.valueOf(k), v));
            return map;
        }
        return Map.of();
    }

    private Object fromJsonOrDefault(String json, Object fallback) {
        if (json == null || json.isBlank()) {
            return fallback;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Object>() {});
        } catch (Exception e) {
            LOGGER.debug("Failed to parse growth context JSON", e);
            return fallback;
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            LOGGER.debug("Failed to serialize growth context JSON", e);
            return null;
        }
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    @Transactional
    public Map<String, Object> resetAssessment(String categoryName) throws Exception {
        User user = authService.getCurrentUser(true);

        if (categoryName != null && !categoryName.isEmpty()) {
            // Reset specific category
            QuestionCategory category = QuestionCategory.valueOf(categoryName.toUpperCase());
            responseRepo.deleteByUserAndCategory(user, category);
        } else {
            // Reset all categories
            responseRepo.deleteByUser(user);
            profileRepo.deleteByUser(user);
        }

        return Map.of("success", true);
    }

    public List<UserAssessmentProfile> findCompatibleProfiles(User user, double tolerance) {
        UserAssessmentProfile myProfile = profileRepo.findByUser(user).orElse(null);
        if (myProfile == null || !Boolean.TRUE.equals(myProfile.getBigFiveComplete())) {
            return Collections.emptyList();
        }

        return profileRepo.findSimilarPersonalities(
                myProfile.getOpennessScore(),
                myProfile.getConscientiousnessScore(),
                myProfile.getExtraversionScore(),
                myProfile.getAgreeablenessScore(),
                myProfile.getEmotionalStabilityScore(),
                tolerance
        );
    }

    public boolean checkDealbreakers(User user1, User user2) {
        UserAssessmentProfile profile1 = profileRepo.findByUser(user1).orElse(null);
        UserAssessmentProfile profile2 = profileRepo.findByUser(user2).orElse(null);

        if (profile1 == null || profile2 == null) {
            return true; // No dealbreaker data, allow match
        }

        Integer flags1 = profile1.getDealbreakerFlags();
        Integer flags2 = profile2.getDealbreakerFlags();

        if (flags1 == null || flags2 == null) {
            return true; // No dealbreaker flags, allow match
        }

        // Dealbreaker logic: if there are critical incompatibilities
        // This is simplified - in production, you'd check specific flag combinations
        return (flags1 & flags2) == 0; // No overlapping critical flags
    }

    /**
     * Calculate OKCupid-style match percentage between two users.
     * Formula: sqrt(user_a_satisfaction * user_b_satisfaction) * 100
     *
     * Each user's satisfaction is calculated based on:
     * - Questions both users have answered
     * - Whether the other user's answer matches what this user finds acceptable
     * - Weighted by the importance each user assigns to each question
     */
    public Map<String, Object> calculateOkCupidMatch(User userA, User userB) {
        List<UserAssessmentResponse> responsesA = responseRepo.findByUser(userA);
        List<UserAssessmentResponse> responsesB = responseRepo.findByUser(userB);

        if (responsesA.isEmpty() || responsesB.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("matchPercentage", 50.0);
            result.put("hasEnoughData", false);
            result.put("commonQuestions", 0);
            result.put("satisfactionA", 50.0);
            result.put("satisfactionB", 50.0);
            result.put("hasMandatoryConflict", false);
            result.put("mandatoryConflicts", List.of());
            result.put("questionMatches", List.of());
            return result;
        }

        // Build maps for quick lookup
        Map<Long, UserAssessmentResponse> responseMapA = responsesA.stream()
                .collect(Collectors.toMap(r -> r.getQuestion().getId(), r -> r));
        Map<Long, UserAssessmentResponse> responseMapB = responsesB.stream()
                .collect(Collectors.toMap(r -> r.getQuestion().getId(), r -> r));

        // Find common questions
        Set<Long> commonQuestionIds = new HashSet<>(responseMapA.keySet());
        commonQuestionIds.retainAll(responseMapB.keySet());

        if (commonQuestionIds.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("matchPercentage", 50.0);
            result.put("hasEnoughData", false);
            result.put("commonQuestions", 0);
            result.put("satisfactionA", 50.0);
            result.put("satisfactionB", 50.0);
            result.put("hasMandatoryConflict", false);
            result.put("mandatoryConflicts", List.of());
            result.put("questionMatches", List.of());
            return result;
        }

        // Calculate satisfaction scores
        double satisfactionA = calculateSatisfaction(responseMapA, responseMapB, commonQuestionIds);
        double satisfactionB = calculateSatisfaction(responseMapB, responseMapA, commonQuestionIds);

        // OKCupid formula: geometric mean of both satisfactions
        double matchPercentage = Math.sqrt(satisfactionA * satisfactionB) * 100;

        // Check for mandatory dealbreakers and build detailed conflict entries
        List<Map<String, Object>> mandatoryConflicts =
                findMandatoryConflicts(responseMapA, responseMapB, commonQuestionIds);
        boolean hasMandatoryConflict = !mandatoryConflicts.isEmpty();
        if (hasMandatoryConflict) {
            matchPercentage = Math.min(matchPercentage, 10.0); // Cap at 10% if mandatory conflict
        }

        List<Map<String, Object>> questionMatches =
                buildQuestionMatches(responseMapA, responseMapB, commonQuestionIds);

        Map<String, Object> result = new HashMap<>();
        result.put("matchPercentage", Math.round(matchPercentage * 10.0) / 10.0);
        result.put("hasEnoughData", commonQuestionIds.size() >= 10);
        result.put("commonQuestions", commonQuestionIds.size());
        result.put("satisfactionA", Math.round(satisfactionA * 1000.0) / 10.0);
        result.put("satisfactionB", Math.round(satisfactionB * 1000.0) / 10.0);
        result.put("hasMandatoryConflict", hasMandatoryConflict);
        result.put("mandatoryConflicts", mandatoryConflicts);
        result.put("questionMatches", questionMatches);
        return result;
    }

    private double calculateSatisfaction(
            Map<Long, UserAssessmentResponse> myResponses,
            Map<Long, UserAssessmentResponse> theirResponses,
            Set<Long> commonQuestionIds) {

        double totalWeight = 0;
        double satisfiedWeight = 0;

        for (Long questionId : commonQuestionIds) {
            UserAssessmentResponse myResponse = myResponses.get(questionId);
            UserAssessmentResponse theirResponse = theirResponses.get(questionId);

            AssessmentQuestion question = myResponse.getQuestion();

            // Get importance weight from user's actual selection (OKCupid-style)
            // Default to "somewhat" = 10 if not specified
            String importanceLevel = myResponse.getImportance();
            if (importanceLevel == null || importanceLevel.isEmpty()) {
                importanceLevel = "somewhat";
            }
            double weight = IMPORTANCE_WEIGHTS.getOrDefault(importanceLevel.toLowerCase(), 10.0);

            // Skip if user marked as irrelevant (weight = 0)
            if (weight == 0) {
                continue;
            }

            // Check if their answer is acceptable to me using OKCupid-style acceptable answers
            boolean satisfied = isAnswerAcceptable(myResponse, theirResponse, question);

            totalWeight += weight;
            if (satisfied) {
                satisfiedWeight += weight;
            }
        }

        return totalWeight > 0 ? satisfiedWeight / totalWeight : 0.5;
    }

    private boolean isAnswerAcceptable(
            UserAssessmentResponse myResponse,
            UserAssessmentResponse theirResponse,
            AssessmentQuestion question) {

        Integer myAnswer = myResponse.getNumericResponse();
        Integer theirAnswer = theirResponse.getNumericResponse();
        String theirText = theirResponse.getTextResponse();

        if (myAnswer == null && (theirAnswer == null && (theirText == null || theirText.isBlank()))) {
            return true; // Can't compare, assume acceptable
        }

        // OKCupid-style: Check against user's explicitly selected acceptable answers
        String acceptableAnswersJson = myResponse.getAcceptableAnswers();
        if (acceptableAnswersJson != null && !acceptableAnswersJson.trim().isEmpty()) {
            try {
                JsonNode acceptable = objectMapper.readTree(acceptableAnswersJson);
                if (acceptable.isArray()) {
                    for (JsonNode node : acceptable) {
                        if (theirAnswer != null) {
                            if (node.isInt() && node.asInt() == theirAnswer) {
                                return true;
                            }
                            if (node.isTextual() && node.asText().equals(String.valueOf(theirAnswer))) {
                                return true;
                            }
                        }
                        if (theirText != null && node.isTextual() &&
                            node.asText().equalsIgnoreCase(theirText.trim())) {
                            return true;
                        }
                    }
                    return false;
                }
            } catch (Exception e) {
                // If JSON parsing fails, fall back to proximity matching
                LOGGER.debug("Failed to parse acceptable answers JSON, using proximity matching", e);
            }
        }

        if (myAnswer == null || theirAnswer == null) {
            return true; // Can't compare numerically, assume acceptable
        }

        // Fallback: same answer or within 1 point is acceptable
        // This is the default behavior for users who haven't set acceptable answers
        int diff = Math.abs(myAnswer - theirAnswer);
        return diff <= 1;
    }

    private List<Map<String, Object>> findMandatoryConflicts(
            Map<Long, UserAssessmentResponse> responsesA,
            Map<Long, UserAssessmentResponse> responsesB,
            Set<Long> commonQuestionIds) {

        List<Map<String, Object>> conflicts = new ArrayList<>();

        for (Long questionId : commonQuestionIds.stream().sorted().collect(Collectors.toList())) {
            UserAssessmentResponse responseA = responsesA.get(questionId);
            UserAssessmentResponse responseB = responsesB.get(questionId);
            if (responseA == null || responseB == null) {
                continue;
            }
            AssessmentQuestion question = responseA.getQuestion();
            List<String> reasons = new ArrayList<>();

            // Check if EITHER user marked this question as mandatory (OKCupid-style)
            boolean aMandatory = "mandatory".equalsIgnoreCase(responseA.getImportance());
            boolean bMandatory = "mandatory".equalsIgnoreCase(responseB.getImportance());

            if (aMandatory && !isAnswerAcceptable(responseA, responseB, question)) {
                reasons.add("YOU_MARKED_MANDATORY");
            }
            if (bMandatory && !isAnswerAcceptable(responseB, responseA, question)) {
                reasons.add("THEY_MARKED_MANDATORY");
            }

            // Also check dealbreaker category questions with critical severity
            if (question.getCategory() == QuestionCategory.DEALBREAKER) {
                Integer answerA = responseA.getNumericResponse();
                Integer answerB = responseB.getNumericResponse();

                if (answerA != null && answerB != null) {
                    // If answers are at opposite extremes on a dealbreaker, it's a conflict
                    if ((answerA == 1 && answerB >= 4) || (answerA >= 4 && answerB == 1)) {
                        if (question.getSeverity() == Severity.CRITICAL) {
                            reasons.add("CRITICAL_DEALBREAKER");
                        }
                    }
                }
            }

            if (!reasons.isEmpty()) {
                Map<String, Object> conflict = new LinkedHashMap<>();
                conflict.put("question", question.getText());
                conflict.put("category", question.getCategory().name());
                conflict.put("yourAnswer", formatAnswer(responseA));
                conflict.put("theirAnswer", formatAnswer(responseB));
                conflict.put("reason", String.join(", ", reasons));
                conflicts.add(conflict);
            }
        }

        return conflicts;
    }

    private List<Map<String, Object>> buildQuestionMatches(
            Map<Long, UserAssessmentResponse> responsesA,
            Map<Long, UserAssessmentResponse> responsesB,
            Set<Long> commonQuestionIds) {

        List<Long> sortedQuestionIds = commonQuestionIds.stream()
                .sorted((q1, q2) -> {
                    UserAssessmentResponse a1 = responsesA.get(q1);
                    UserAssessmentResponse b1 = responsesB.get(q1);
                    UserAssessmentResponse a2 = responsesA.get(q2);
                    UserAssessmentResponse b2 = responsesB.get(q2);
                    int rank1 = Math.max(importanceRank(a1 != null ? a1.getImportance() : null),
                            importanceRank(b1 != null ? b1.getImportance() : null));
                    int rank2 = Math.max(importanceRank(a2 != null ? a2.getImportance() : null),
                            importanceRank(b2 != null ? b2.getImportance() : null));
                    if (rank1 != rank2) {
                        return Integer.compare(rank2, rank1);
                    }
                    return Long.compare(q1, q2);
                })
                .limit(200)
                .collect(Collectors.toList());

        List<Map<String, Object>> matches = new ArrayList<>();
        for (Long questionId : sortedQuestionIds) {
            UserAssessmentResponse responseA = responsesA.get(questionId);
            UserAssessmentResponse responseB = responsesB.get(questionId);
            if (responseA == null || responseB == null) {
                continue;
            }

            AssessmentQuestion question = responseA.getQuestion();
            boolean aSatisfied = isAnswerAcceptable(responseA, responseB, question);
            boolean bSatisfied = isAnswerAcceptable(responseB, responseA, question);
            boolean isMatch = aSatisfied && bSatisfied;
            boolean isPartial = false;

            if (!isMatch &&
                responseA.getNumericResponse() != null &&
                responseB.getNumericResponse() != null) {
                isPartial = Math.abs(responseA.getNumericResponse() - responseB.getNumericResponse()) == 1;
            }

            String mergedImportance = importanceRank(responseA.getImportance()) >= importanceRank(responseB.getImportance())
                    ? responseA.getImportance()
                    : responseB.getImportance();

            Map<String, Object> questionMatch = new LinkedHashMap<>();
            questionMatch.put("questionText", question.getText());
            questionMatch.put("yourAnswer", formatAnswer(responseA));
            questionMatch.put("theirAnswer", formatAnswer(responseB));
            questionMatch.put("yourImportance", normalizeImportance(responseA.getImportance()));
            questionMatch.put("theirImportance", normalizeImportance(responseB.getImportance()));
            questionMatch.put("isMatch", isMatch);
            questionMatch.put("isPartialMatch", isPartial);
            questionMatch.put("importance", normalizeImportance(mergedImportance));
            matches.add(questionMatch);
        }

        return matches;
    }

    private String formatAnswer(UserAssessmentResponse response) {
        if (response == null) {
            return "";
        }
        if (response.getTextResponse() != null && !response.getTextResponse().isBlank()) {
            return response.getTextResponse().trim();
        }
        if (response.getNumericResponse() == null) {
            return "";
        }
        String optionText = resolveOptionText(response.getQuestion(), response.getNumericResponse());
        return optionText != null ? optionText : String.valueOf(response.getNumericResponse());
    }

    private String resolveOptionText(AssessmentQuestion question, Integer numericValue) {
        if (question == null || numericValue == null || question.getOptions() == null || question.getOptions().isBlank()) {
            return null;
        }
        try {
            JsonNode options = objectMapper.readTree(question.getOptions());
            if (!options.isArray() || options.isEmpty()) {
                return null;
            }

            int index = numericValue - 1;
            if (index >= 0 && index < options.size()) {
                String indexedText = extractOptionText(options.get(index));
                if (indexedText != null) {
                    return indexedText;
                }
            }

            for (JsonNode option : options) {
                JsonNode idNode = option.get("id");
                if (idNode != null && idNode.asText().equals(String.valueOf(numericValue))) {
                    return extractOptionText(option);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not parse question options for {}", question.getExternalId(), e);
        }
        return null;
    }

    private String extractOptionText(JsonNode option) {
        if (option == null || option.isNull()) {
            return null;
        }
        if (option.has("text")) return option.get("text").asText();
        if (option.has("label")) return option.get("label").asText();
        if (option.has("value")) return option.get("value").asText();
        if (option.has("title")) return option.get("title").asText();
        if (option.has("id")) return option.get("id").asText();
        if (option.isTextual()) return option.asText();
        return null;
    }

    private String normalizeImportance(String importance) {
        if (importance == null || importance.isBlank()) {
            return "somewhat";
        }
        return importance.trim().toLowerCase();
    }

    private int importanceRank(String importance) {
        return switch (normalizeImportance(importance)) {
            case "mandatory" -> 5;
            case "very" -> 4;
            case "somewhat" -> 3;
            case "a_little" -> 2;
            case "irrelevant" -> 1;
            default -> 3;
        };
    }

    /**
     * Get match explanation with detailed breakdown
     */
    public Map<String, Object> getMatchExplanation(User userA, User userB) {
        Map<String, Object> match = calculateOkCupidMatch(userA, userB);

        List<UserAssessmentResponse> responsesA = responseRepo.findByUser(userA);
        List<UserAssessmentResponse> responsesB = responseRepo.findByUser(userB);

        // Calculate category-level compatibility
        Map<String, Double> categoryScores = new HashMap<>();
        for (QuestionCategory category : QuestionCategory.values()) {
            double score = calculateCategoryCompatibility(responsesA, responsesB, category);
            if (score >= 0) {
                categoryScores.put(category.name(), Math.round(score * 10.0) / 10.0);
            }
        }

        Map<String, Object> explanation = new HashMap<>(match);
        explanation.put("categoryBreakdown", categoryScores);

        return explanation;
    }

    private double calculateCategoryCompatibility(
            List<UserAssessmentResponse> responsesA,
            List<UserAssessmentResponse> responsesB,
            QuestionCategory category) {

        List<UserAssessmentResponse> catResponsesA = responsesA.stream()
                .filter(r -> r.getCategory() == category)
                .collect(Collectors.toList());
        List<UserAssessmentResponse> catResponsesB = responsesB.stream()
                .filter(r -> r.getCategory() == category)
                .collect(Collectors.toList());

        if (catResponsesA.isEmpty() || catResponsesB.isEmpty()) {
            return -1; // Not enough data
        }

        Map<Long, UserAssessmentResponse> mapA = catResponsesA.stream()
                .collect(Collectors.toMap(r -> r.getQuestion().getId(), r -> r));
        Map<Long, UserAssessmentResponse> mapB = catResponsesB.stream()
                .collect(Collectors.toMap(r -> r.getQuestion().getId(), r -> r));

        Set<Long> common = new HashSet<>(mapA.keySet());
        common.retainAll(mapB.keySet());

        if (common.isEmpty()) {
            return -1;
        }

        double satA = calculateSatisfaction(mapA, mapB, common);
        double satB = calculateSatisfaction(mapB, mapA, common);

        return Math.sqrt(satA * satB) * 100;
    }

    // ============== Question Bank API Methods ==============

    /**
     * Get the next unanswered question for the current user.
     * Questions are prioritized by:
     * 1. Core questions (for intake flow)
     * 2. Category order (BIG_FIVE, ATTACHMENT, DEALBREAKER, VALUES, LIFESTYLE)
     * 3. Display order within category
     *
     * @param category Optional category filter
     * @return Next question or null if all answered
     */
    public Map<String, Object> getNextQuestion(String category) throws Exception {
        User user = authService.getCurrentUser(true);

        // Get all answered question IDs for this user
        List<UserAssessmentResponse> responses = responseRepo.findByUser(user);
        Set<Long> answeredIds = responses.stream()
                .map(r -> r.getQuestion().getId())
                .collect(Collectors.toSet());

        List<AssessmentQuestion> candidates;

        if (category != null && !category.isEmpty()) {
            // Filter by category
            QuestionCategory cat = QuestionCategory.valueOf(category.toUpperCase());
            candidates = questionRepo.findActiveQuestionsByCategory(cat);
        } else {
            // Get all questions, prioritizing core questions first
            candidates = questionRepo.findByActiveTrueOrderByDisplayOrderAsc();
        }

        // Find first unanswered question (prioritize core questions)
        AssessmentQuestion nextQuestion = candidates.stream()
                .filter(q -> !answeredIds.contains(q.getId()))
                .sorted((a, b) -> {
                    // Core questions first
                    boolean aCore = Boolean.TRUE.equals(a.getCoreQuestion());
                    boolean bCore = Boolean.TRUE.equals(b.getCoreQuestion());
                    if (aCore != bCore) return aCore ? -1 : 1;
                    // Then by display order
                    return Integer.compare(a.getDisplayOrder(), b.getDisplayOrder());
                })
                .findFirst()
                .orElse(null);

        if (nextQuestion == null) {
            return Map.of(
                    "hasNext", false,
                    "message", "All questions in this category have been answered",
                    "totalAnswered", answeredIds.size()
            );
        }

        return buildQuestionResponse(nextQuestion, answeredIds.size(), candidates.size());
    }

    /**
     * Get a batch of unanswered questions for the user.
     *
     * @param category Optional category filter
     * @param limit Maximum number of questions to return
     * @return List of unanswered questions
     */
    public Map<String, Object> getNextUnansweredQuestions(String category, int limit) throws Exception {
        User user = authService.getCurrentUser(true);

        Set<Long> answeredIds = responseRepo.findByUser(user).stream()
                .map(r -> r.getQuestion().getId())
                .collect(Collectors.toSet());

        List<AssessmentQuestion> candidates;

        if (category != null && !category.isEmpty()) {
            QuestionCategory cat = QuestionCategory.valueOf(category.toUpperCase());
            candidates = questionRepo.findActiveQuestionsByCategory(cat);
        } else {
            candidates = questionRepo.findByActiveTrueOrderByDisplayOrderAsc();
        }

        List<Map<String, Object>> questions = candidates.stream()
                .filter(q -> !answeredIds.contains(q.getId()))
                .sorted((a, b) -> {
                    boolean aCore = Boolean.TRUE.equals(a.getCoreQuestion());
                    boolean bCore = Boolean.TRUE.equals(b.getCoreQuestion());
                    if (aCore != bCore) return aCore ? -1 : 1;
                    return Integer.compare(a.getDisplayOrder(), b.getDisplayOrder());
                })
                .limit(limit)
                .map(q -> buildQuestionData(q))
                .collect(Collectors.toList());

        long totalUnanswered = candidates.stream()
                .filter(q -> !answeredIds.contains(q.getId()))
                .count();

        return Map.of(
                "questions", questions,
                "totalUnanswered", totalUnanswered,
                "totalAnswered", answeredIds.size(),
                "hasMore", totalUnanswered > limit
        );
    }

    /**
     * Validate an answer before saving.
     * Checks:
     * - Question exists and is active
     * - Response is within valid range for the response scale
     * - Required fields are present
     *
     * @param questionId External question ID
     * @param numericResponse Numeric response (1-5 for Likert, 0-1 for binary)
     * @param textResponse Text response for free-text questions
     * @return Validation result with any errors
     */
    public Map<String, Object> validateAnswer(String questionId, Integer numericResponse, String textResponse) {
        Optional<AssessmentQuestion> questionOpt = questionRepo.findByExternalId(questionId);

        if (questionOpt.isEmpty()) {
            return Map.of(
                    "valid", false,
                    "error", "Question not found: " + questionId
            );
        }

        AssessmentQuestion question = questionOpt.get();

        if (!Boolean.TRUE.equals(question.getActive())) {
            return Map.of(
                    "valid", false,
                    "error", "Question is no longer active"
            );
        }

        // Validate based on response scale
        ResponseScale scale = question.getResponseScale();

        switch (scale) {
            case LIKERT_5, AGREEMENT_5, FREQUENCY_5:
                if (numericResponse == null) {
                    return Map.of("valid", false, "error", "Numeric response required");
                }
                int maxAllowed = getNumericUpperBound(question);
                if (numericResponse < 1 || numericResponse > maxAllowed) {
                    return Map.of("valid", false, "error", "Response must be between 1 and " + maxAllowed);
                }
                break;

            case BINARY:
                if (numericResponse == null) {
                    return Map.of("valid", false, "error", "Response required (0 or 1)");
                }
                if (numericResponse != 0 && numericResponse != 1) {
                    return Map.of("valid", false, "error", "Binary response must be 0 or 1");
                }
                break;

            case FREE_TEXT:
                if (textResponse == null || textResponse.trim().isEmpty()) {
                    return Map.of("valid", false, "error", "Text response required");
                }
                if (textResponse.length() > 2000) {
                    return Map.of("valid", false, "error", "Response too long (max 2000 characters)");
                }
                break;
        }

        return Map.of(
                "valid", true,
                "questionId", questionId,
                "responseScale", scale.name()
        );
    }

    private int getNumericUpperBound(AssessmentQuestion question) {
        if (question.getOptions() == null || question.getOptions().isBlank()) {
            return 5;
        }
        try {
            JsonNode options = objectMapper.readTree(question.getOptions());
            if (options.isArray() && !options.isEmpty()) {
                return options.size();
            }
        } catch (Exception e) {
            LOGGER.debug("Could not parse options for numeric bounds, defaulting to 5", e);
        }
        return 5;
    }

    /**
     * Submit a single answer with validation.
     * OKCupid-style: includes importance weighting and acceptable answers.
     */
    @Transactional
    public Map<String, Object> submitSingleAnswer(String questionId, Integer numericResponse,
                                                   String textResponse, String importance,
                                                   String acceptableAnswers, String explanation,
                                                   Boolean publicVisible) throws Exception {
        // Validate first
        Map<String, Object> validation = validateAnswer(questionId, numericResponse, textResponse);
        if (!Boolean.TRUE.equals(validation.get("valid"))) {
            return validation;
        }

        // Create DTO and submit with all OKCupid-style fields
        AssessmentResponseDto dto = new AssessmentResponseDto();
        dto.setQuestionId(questionId);
        dto.setNumericResponse(numericResponse);
        dto.setTextResponse(textResponse);
        if (importance != null) {
            dto.setImportance(importance);
        }
        if (acceptableAnswers != null) {
            dto.setAcceptableAnswers(acceptableAnswers);
        }
        if (explanation != null) {
            dto.setExplanation(explanation);
        }
        if (publicVisible != null) {
            dto.setPublicVisible(publicVisible);
        }

        return submitResponses(List.of(dto));
    }

    /**
     * Convenience method for basic answer submission (backwards compatibility).
     */
    @Transactional
    public Map<String, Object> submitSingleAnswer(String questionId, Integer numericResponse,
                                                   String textResponse, String importance) throws Exception {
        return submitSingleAnswer(questionId, numericResponse, textResponse, importance, null, null, null);
    }

    private Map<String, Object> buildQuestionResponse(AssessmentQuestion question, int answered, int total) {
        Map<String, Object> response = buildQuestionData(question);
        response.put("hasNext", true);
        response.put("progress", Map.of(
                "answered", answered,
                "total", total,
                "percentage", total > 0 ? (answered * 100.0 / total) : 0
        ));
        return response;
    }

    private Map<String, Object> buildQuestionData(AssessmentQuestion q) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", q.getId());
        data.put("externalId", q.getExternalId());
        data.put("text", q.getText());
        data.put("category", q.getCategory().name());
        data.put("responseScale", q.getResponseScale().name());
        data.put("isCore", Boolean.TRUE.equals(q.getCoreQuestion()));

        if (q.getSubcategory() != null) data.put("subcategory", q.getSubcategory());
        if (q.getDomain() != null) data.put("domain", q.getDomain());
        if (q.getFacet() != null) data.put("facet", q.getFacet());
        if (q.getOptions() != null) {
            try {
                data.put("options", objectMapper.readTree(q.getOptions()));
            } catch (Exception e) {
                data.put("options", q.getOptions());
            }
        }
        if (q.getSuggestedImportance() != null) {
            data.put("suggestedImportance", q.getSuggestedImportance());
        }

        return data;
    }
}
