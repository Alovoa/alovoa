package com.nonononoki.alovoa.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.entity.CompatibilityScore;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.UserAssessmentProfile;
import com.nonononoki.alovoa.entity.user.UserDailyMatchLimit;
import com.nonononoki.alovoa.entity.user.UserPersonalityProfile;
import com.nonononoki.alovoa.model.MatchRecommendationDto;
import com.nonononoki.alovoa.model.CompatibilityExplanationDto;
import com.nonononoki.alovoa.entity.user.UserLocationPreferences;
import com.nonononoki.alovoa.repo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MatchingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MatchingService.class);

    @Value("${app.aura.ai-service.url:http://localhost:8002}")
    private String aiServiceUrl;

    @Value("${app.aura.daily-match.limit.default:5}")
    private Integer dailyMatchLimit;

    @Value("${app.aura.compatibility.minimum:50}")
    private Integer minimumCompatibility;

    @Value("${app.aura.political-assessment.required:true}")
    private Boolean politicalAssessmentRequired;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private CompatibilityScoreRepository compatibilityRepo;

    @Autowired
    private UserDailyMatchLimitRepository matchLimitRepo;

    @Autowired
    private UserPersonalityProfileRepository personalityRepo;

    @Autowired
    private PoliticalAssessmentService politicalAssessmentService;

    @Autowired
    private IntakeService intakeService;

    @Autowired
    private UserAssessmentProfileRepository assessmentProfileRepo;

    @Autowired
    private AssessmentService assessmentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TravelTimeService travelTimeService;

    @Autowired
    private LocationAreaService locationAreaService;

    @Autowired
    private UserLocationPreferencesRepository locationPrefsRepo;

    @Autowired
    private DonationService donationService;

    public Map<String, Object> getDailyMatches() throws Exception {
        User user = authService.getCurrentUser(true);

        // Check intake completion gate before allowing matching
        if (!intakeService.isIntakeComplete(user)) {
            return Map.of(
                    "matches", Collections.emptyList(),
                    "gated", true,
                    "gateMessage", "Complete your profile intake to start matching. Answer the 10 core questions and upload your video introduction.",
                    "gateStatus", "INTAKE_INCOMPLETE",
                    "intakeRequired", true
            );
        }

        // Check political assessment gate before allowing matching
        if (politicalAssessmentRequired && !politicalAssessmentService.canAccessMatching(user)) {
            String message = politicalAssessmentService.getGateStatusMessage(user);
            return Map.of(
                    "matches", Collections.emptyList(),
                    "gated", true,
                    "gateMessage", message,
                    "gateStatus", user.getPoliticalGateStatus().name()
            );
        }

        Date today = truncateToDay(new Date());

        UserDailyMatchLimit matchLimit = matchLimitRepo
                .findByUserAndMatchDate(user, today)
                .orElseGet(() -> createNewDailyLimit(user, today));

        if (matchLimit.hasReachedLimit()) {
            return Map.of(
                    "matches", Collections.emptyList(),
                    "dailyLimitReached", true,
                    "remaining", 0,
                    "resetsAt", getNextMidnight()
            );
        }

        int remaining = matchLimit.getRemainingMatches();
        List<MatchRecommendationDto> matches;

        try {
            // Try to call AI service
            matches = callAIMatchingService(user, remaining);
        } catch (Exception e) {
            LOGGER.warn("AI service unavailable, using fallback matching", e);
            matches = getFallbackMatches(user, remaining);
        }

        // Update daily limit
        matchLimit.setMatchesShown(matchLimit.getMatchesShown() + matches.size());
        updateShownUserIds(matchLimit, matches);
        matchLimitRepo.save(matchLimit);

        return Map.of(
                "matches", matches,
                "remaining", matchLimit.getRemainingMatches(),
                "dailyLimitReached", matchLimit.hasReachedLimit()
        );
    }

    /**
     * Return daily limit metadata without generating a new batch of matches.
     * This is used by mobile clients that fetch limit + matches via separate endpoints.
     */
    public Map<String, Object> getDailyLimitStatus() throws Exception {
        User user = authService.getCurrentUser(true);
        Date today = truncateToDay(new Date());

        UserDailyMatchLimit matchLimit = matchLimitRepo
                .findByUserAndMatchDate(user, today)
                .orElseGet(() -> createNewDailyLimit(user, today));

        return Map.of(
                "remaining", matchLimit.getRemainingMatches(),
                "limit", dailyMatchLimit,
                "dailyLimitReached", matchLimit.hasReachedLimit(),
                "resetsAt", getNextMidnight()
        );
    }

    @SuppressWarnings("unchecked")
    public CompatibilityExplanationDto getCompatibilityExplanation(String matchUuid) throws Exception {
        User user = authService.getCurrentUser(true);
        User matchUser = userRepo.findByUuid(UUID.fromString(matchUuid));
        if (matchUser == null) {
            throw new Exception("User not found");
        }

        CompatibilityScore compatibility = compatibilityRepo
                .findByUserAAndUserB(user, matchUser)
                .map(existing -> isCompatibilityStale(existing, user, matchUser)
                        ? calculateAndStoreCompatibility(user, matchUser, existing)
                        : existing)
                .orElseGet(() -> calculateAndStoreCompatibility(user, matchUser, null));

        CompatibilityExplanationDto dto = new CompatibilityExplanationDto();

        // === Basic Info ===
        dto.setMatchName(matchUser.getFirstName());

        // === Core Scores ===
        dto.setOverallScore(compatibility.getOverallScore());
        dto.setEnemyScore(compatibility.getEnemyScore() != null ? compatibility.getEnemyScore() : 0.0);

        // === OKCupid-style Match Percentage (Marriage Machine) ===
        try {
            Map<String, Object> matchData = assessmentService.calculateOkCupidMatch(user, matchUser);

            // Match percentage using geometric mean (deprecated for display)
            Double matchPercentage = (Double) matchData.get("matchPercentage");
            dto.setMatchPercentage(matchPercentage != null ? matchPercentage : compatibility.getOverallScore());

            // Set human-readable match category label (for user-facing display)
            dto.setMatchCategoryLabel(scoreToCategory(dto.getMatchPercentage()));

            // Bidirectional satisfaction scores
            // satisfactionA = how much B matches what A wants (A's satisfaction with B)
            // satisfactionB = how much A matches what B wants (B's satisfaction with A)
            // From currentUser's perspective: yourSatisfaction = satisfactionA, theirSatisfaction = satisfactionB
            Double satisfactionA = (Double) matchData.get("satisfactionA");
            Double satisfactionB = (Double) matchData.get("satisfactionB");
            // Convert from percentage (0-100) to ratio (0-1) for frontend display
            dto.setYourSatisfaction(satisfactionA != null ? satisfactionA / 100.0 : 0.5);
            dto.setTheirSatisfaction(satisfactionB != null ? satisfactionB / 100.0 : 0.5);

            // Questions compared
            Integer commonQuestions = (Integer) matchData.get("commonQuestions");
            dto.setQuestionsCompared(commonQuestions != null ? commonQuestions : 0);

            // Dealbreaker detection
            Boolean hasMandatoryConflict = (Boolean) matchData.get("hasMandatoryConflict");
            dto.setHasDealbreaker(hasMandatoryConflict != null ? hasMandatoryConflict : false);
            dto.setMandatoryConflicts(dto.getHasDealbreaker());

            // Get dealbreaker details if present
            if (Boolean.TRUE.equals(dto.getHasDealbreaker())) {
                List<Map<String, Object>> conflicts = (List<Map<String, Object>>) matchData.get("mandatoryConflicts");
                if (conflicts != null) {
                    List<CompatibilityExplanationDto.DealbreakderDetail> dealbreakers = new ArrayList<>();
                    for (Map<String, Object> conflict : conflicts) {
                        CompatibilityExplanationDto.DealbreakderDetail detail = new CompatibilityExplanationDto.DealbreakderDetail();
                        detail.setQuestion((String) conflict.get("question"));
                        detail.setCategory((String) conflict.get("category"));
                        detail.setYourAnswer((String) conflict.get("yourAnswer"));
                        detail.setTheirAnswer((String) conflict.get("theirAnswer"));
                        dealbreakers.add(detail);
                    }
                    dto.setDealbreakers(dealbreakers);
                }
            }

            // Get question-by-question matches
            List<Map<String, Object>> questionMatchData = (List<Map<String, Object>>) matchData.get("questionMatches");
            if (questionMatchData != null) {
                List<CompatibilityExplanationDto.QuestionMatch> questionMatches = new ArrayList<>();
                for (Map<String, Object> qm : questionMatchData) {
                    CompatibilityExplanationDto.QuestionMatch match = new CompatibilityExplanationDto.QuestionMatch();
                    match.setQuestionText((String) qm.get("questionText"));
                    match.setYourAnswer((String) qm.get("yourAnswer"));
                    match.setTheirAnswer((String) qm.get("theirAnswer"));
                    match.setYourImportance((String) qm.get("yourImportance"));
                    match.setTheirImportance((String) qm.get("theirImportance"));
                    match.setIsMatch((Boolean) qm.get("isMatch"));
                    match.setIsPartialMatch((Boolean) qm.get("isPartialMatch"));
                    match.setImportance((String) qm.get("importance"));
                    questionMatches.add(match);
                }
                dto.setQuestionMatches(questionMatches);
            }

        } catch (Exception e) {
            LOGGER.warn("Failed to get OKCupid match data, using defaults", e);
            dto.setMatchPercentage(compatibility.getOverallScore());
            dto.setMatchCategoryLabel(scoreToCategory(compatibility.getOverallScore()));
            dto.setYourSatisfaction(0.5);
            dto.setTheirSatisfaction(0.5);
            dto.setQuestionsCompared(0);
            dto.setHasDealbreaker(false);
        }

        // === Category Breakdown (Marriage Machine categories) ===
        try {
            Map<String, Object> explanation = assessmentService.getMatchExplanation(user, matchUser);
            Map<String, Double> categoryBreakdown = (Map<String, Double>) explanation.get("categoryBreakdown");
            if (categoryBreakdown != null && !categoryBreakdown.isEmpty()) {
                dto.setCategoryBreakdown(categoryBreakdown);
                // Generate dimension labels (non-numerical) for user display
                dto.setDimensionLabels(generateCategoryLabels(categoryBreakdown));
            }
        } catch (Exception e) {
            LOGGER.debug("Could not get category breakdown", e);
        }

        // === Legacy Dimension Scores (for backward compatibility) ===
        Map<String, Double> dimensionScores = new HashMap<>();
        dimensionScores.put("values", compatibility.getValuesScore() != null ? compatibility.getValuesScore() : 50.0);
        dimensionScores.put("lifestyle", compatibility.getLifestyleScore() != null ? compatibility.getLifestyleScore() : 50.0);
        dimensionScores.put("personality", compatibility.getPersonalityScore() != null ? compatibility.getPersonalityScore() : 50.0);
        dimensionScores.put("attraction", compatibility.getAttractionScore() != null ? compatibility.getAttractionScore() : 50.0);
        dimensionScores.put("circumstantial", compatibility.getCircumstantialScore() != null ? compatibility.getCircumstantialScore() : 50.0);
        dimensionScores.put("growth", compatibility.getGrowthScore() != null ? compatibility.getGrowthScore() : 50.0);
        dto.setDimensionScores(dimensionScores);

        // === Top Compatibilities ===
        if (compatibility.getTopCompatibilities() != null && !compatibility.getTopCompatibilities().isEmpty()) {
            try {
                List<String> compatibilities = objectMapper.readValue(
                        compatibility.getTopCompatibilities(),
                        new TypeReference<List<String>>() {}
                );
                dto.setTopCompatibilities(compatibilities);
            } catch (JsonProcessingException e) {
                dto.setTopCompatibilities(Arrays.asList(compatibility.getTopCompatibilities().split("\n")));
            }
        } else {
            dto.setTopCompatibilities(generateDefaultCompatibilities(compatibility));
        }

        // === Potential Challenges / Areas to Discuss ===
        List<String> challenges;
        if (compatibility.getPotentialChallenges() != null && !compatibility.getPotentialChallenges().isEmpty()) {
            try {
                challenges = objectMapper.readValue(
                        compatibility.getPotentialChallenges(),
                        new TypeReference<List<String>>() {}
                );
            } catch (JsonProcessingException e) {
                challenges = Arrays.asList(compatibility.getPotentialChallenges().split("\n"));
            }
        } else {
            challenges = generateDefaultChallenges(compatibility);
        }
        dto.setPotentialChallenges(challenges);
        dto.setAreasToDiscuss(challenges);  // Alias for frontend

        // === Match Insight Object ===
        CompatibilityExplanationDto.MatchInsight insight = new CompatibilityExplanationDto.MatchInsight();
        insight.setTopAreas(dto.getTopCompatibilities());
        insight.setAreasToDiscuss(challenges);
        insight.setSummary(generateMatchInsightSummary(dto.getMatchPercentage(), dto.getHasDealbreaker()));
        dto.setMatchInsight(insight);

        // === Detailed Explanation JSON ===
        if (compatibility.getExplanationJson() != null) {
            try {
                Map<String, Object> explanation = objectMapper.readValue(
                        compatibility.getExplanationJson(),
                        new TypeReference<Map<String, Object>>() {}
                );
                dto.setDetailedExplanation(explanation);
            } catch (JsonProcessingException e) {
                LOGGER.error("Failed to parse explanation JSON", e);
            }
        }

        // === Summary ===
        dto.setSummary(generateCompatibilitySummary(compatibility));

        return dto;
    }

    /**
     * Generate a brief match insight summary
     */
    private String generateMatchInsightSummary(Double matchPercentage, Boolean hasDealbreaker) {
        if (Boolean.TRUE.equals(hasDealbreaker)) {
            return "Potential dealbreaker detected - review compatibility carefully";
        }
        if (matchPercentage == null) {
            return "Answer more questions to improve match accuracy";
        }
        if (matchPercentage >= 90) {
            return "Exceptional compatibility! Strong alignment across all areas";
        } else if (matchPercentage >= 80) {
            return "High compatibility - you share important values and perspectives";
        } else if (matchPercentage >= 70) {
            return "Good compatibility - similar outlook with room for discovery";
        } else if (matchPercentage >= 60) {
            return "Moderate compatibility - some differences could add variety";
        } else {
            return "Different perspectives - could challenge each other in healthy ways";
        }
    }

    /**
     * Generate default compatibility strengths when not provided by AI service
     */
    private List<String> generateDefaultCompatibilities(CompatibilityScore score) {
        List<String> compatibilities = new ArrayList<>();

        if (score.getValuesScore() != null && score.getValuesScore() >= 70) {
            compatibilities.add("Strong alignment in core values and life priorities");
        }
        if (score.getPersonalityScore() != null && score.getPersonalityScore() >= 70) {
            compatibilities.add("Compatible personality traits and communication styles");
        }
        if (score.getLifestyleScore() != null && score.getLifestyleScore() >= 70) {
            compatibilities.add("Similar lifestyle preferences and daily routines");
        }
        if (score.getGrowthScore() != null && score.getGrowthScore() >= 70) {
            compatibilities.add("Shared approach to personal growth and development");
        }

        if (compatibilities.isEmpty()) {
            compatibilities.add("You both have unique qualities that could complement each other");
        }

        return compatibilities;
    }

    /**
     * Generate default challenges when not provided by AI service
     */
    private List<String> generateDefaultChallenges(CompatibilityScore score) {
        List<String> challenges = new ArrayList<>();

        if (score.getValuesScore() != null && score.getValuesScore() < 50) {
            challenges.add("Different perspectives on core values may require open communication");
        }
        if (score.getPersonalityScore() != null && score.getPersonalityScore() < 50) {
            challenges.add("Personality differences may need understanding and compromise");
        }
        if (score.getLifestyleScore() != null && score.getLifestyleScore() < 50) {
            challenges.add("Lifestyle preferences may differ and require coordination");
        }
        if (score.getEnemyScore() != null && score.getEnemyScore() > 30) {
            challenges.add("Some fundamental differences exist that will require patience and understanding");
        }

        if (challenges.isEmpty()) {
            challenges.add("Communication and mutual respect will help navigate any differences");
        }

        return challenges;
    }

    /**
     * Generate a human-readable summary of compatibility
     */
    private String generateCompatibilitySummary(CompatibilityScore score) {
        if (score.getOverallScore() >= 80) {
            return "You two have exceptional compatibility! Strong alignment across multiple dimensions suggests a great potential connection.";
        } else if (score.getOverallScore() >= 70) {
            return "You have strong compatibility with this person. Your core values and personalities align well, creating a solid foundation for connection.";
        } else if (score.getOverallScore() >= 60) {
            return "You have good compatibility with this person. While you share important similarities, some differences could add interesting dynamics to your relationship.";
        } else if (score.getOverallScore() >= 50) {
            return "You have moderate compatibility. There are both shared interests and differences that could either complement or challenge you.";
        } else {
            return "You have noticeable differences in key areas. While opposites can attract, this match may require extra effort to understand each other.";
        }
    }

    private List<MatchRecommendationDto> callAIMatchingService(User user, int limit) {
        try {
            String url = aiServiceUrl + "/match/daily?user_id=" + user.getId() + "&limit=" + limit;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = buildUserProfileForMatching(user);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<List> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    List.class
            );

            if (response.getBody() != null) {
                return convertToMatchRecommendations(response.getBody());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to call AI matching service", e);
        }
        return Collections.emptyList();
    }

    private List<MatchRecommendationDto> getFallbackMatches(User user, int limit) {
        // Simple fallback: get random users who match basic criteria
        List<MatchRecommendationDto> matches = new ArrayList<>();

        // Get user's location preferences
        UserLocationPreferences locationPrefs = locationPrefsRepo.findByUser(user).orElse(null);
        int maxTravelMinutes = locationPrefs != null ? locationPrefs.getMaxTravelMinutes() : 60;
        boolean requireOverlap = locationPrefs != null ? locationPrefs.isRequireAreaOverlap() : false;
        boolean showExceptional = locationPrefs != null ? locationPrefs.isShowExceptionalMatches() : true;
        double exceptionalThreshold = locationPrefs != null ? locationPrefs.getExceptionalMatchThreshold() : 0.90;

        // Use existing compatibility scores if available
        List<CompatibilityScore> cachedScores = compatibilityRepo
                .findByUserAOrderByOverallScoreDesc(user);

        for (CompatibilityScore cachedScore : cachedScores) {
            if (matches.size() >= limit) break;
            User matchUser = cachedScore.getUserB();
            CompatibilityScore score = cachedScore;
            if (isCompatibilityStale(cachedScore, user, matchUser)) {
                score = calculateAndStoreCompatibility(user, matchUser, cachedScore);
            }
            if (score.getOverallScore() != null && score.getOverallScore() >= minimumCompatibility) {
                // Apply location filtering
                TravelTimeService.TravelTimeInfo travelInfo = travelTimeService.getTravelTimeInfo(user, matchUser);

                // Check if match is within travel time preferences
                boolean locationMatch = true;
                if (travelInfo.getMinutes() >= 0 && travelInfo.getMinutes() > maxTravelMinutes) {
                    // Outside travel preferences
                    if (showExceptional && score.getOverallScore() >= (exceptionalThreshold * 100)) {
                        // Exceptional match - include anyway but flag it
                        locationMatch = true;
                    } else if (requireOverlap && !travelInfo.isHasOverlappingAreas()) {
                        // Requires overlap but none found
                        locationMatch = false;
                    } else if (requireOverlap) {
                        // Has overlap even if travel time is high
                        locationMatch = true;
                    } else {
                        locationMatch = false;
                    }
                }

                if (locationMatch) {
                    MatchRecommendationDto dto = new MatchRecommendationDto();
                    dto.setUserId(matchUser.getId());
                    dto.setUserUuid(matchUser.getUuid().toString());
                    dto.setCompatibilityScore(score.getOverallScore());
                    dto.setEnemyScore(score.getEnemyScore());

                    // Add travel time info to match
                    dto.setTravelTimeMinutes(travelInfo.getMinutes());
                    dto.setTravelTimeDisplay(travelInfo.getDisplay());
                    dto.setHasOverlappingAreas(travelInfo.isHasOverlappingAreas());
                    dto.setOverlappingAreas(travelInfo.getOverlappingAreas());

                    // Add OKCupid-style match percentage (Marriage Machine feature)
                    populateOkCupidMatchData(dto, user, matchUser);

                    matches.add(dto);
                }
            }
        }

        return matches;
    }

    private CompatibilityScore calculateAndStoreCompatibility(User userA, User userB, CompatibilityScore existingScore) {
        CompatibilityScore score = existingScore != null ? existingScore : new CompatibilityScore();
        score.setUserA(userA);
        score.setUserB(userB);
        score.setCalculatedAt(new Date());
        score.setUserAProfileUpdatedAt(getAssessmentProfileUpdatedAt(userA));
        score.setUserBProfileUpdatedAt(getAssessmentProfileUpdatedAt(userB));

        // Try to call AI service for calculation
        try {
            Map<String, Object> result = callAICompatibilityService(userA, userB);
            score.setValuesScore((Double) result.getOrDefault("values", 50.0));
            score.setLifestyleScore((Double) result.getOrDefault("lifestyle", 50.0));
            score.setPersonalityScore((Double) result.getOrDefault("personality", 50.0));
            score.setAttractionScore((Double) result.getOrDefault("attraction", 50.0));
            score.setCircumstantialScore((Double) result.getOrDefault("circumstantial", 50.0));
            score.setGrowthScore((Double) result.getOrDefault("growth", 50.0));
            score.setOverallScore((Double) result.getOrDefault("overall", 50.0));
            score.setEnemyScore(0.0);
            if (result.containsKey("enemy")) {
                Object enemy = result.get("enemy");
                if (enemy instanceof Number number) {
                    score.setEnemyScore(number.doubleValue());
                }
            }
            if (result.containsKey("explanation")) {
                score.setExplanationJson(objectMapper.writeValueAsString(result.get("explanation")));
            } else {
                score.setExplanationJson(null);
            }
        } catch (Exception e) {
            LOGGER.warn("AI service unavailable, using personality-based calculation", e);
            calculatePersonalityBasedCompatibility(score, userA, userB);
        }

        return compatibilityRepo.save(score);
    }

    private boolean isCompatibilityStale(CompatibilityScore score, User userA, User userB) {
        if (score == null) {
            return true;
        }
        if (score.getUserAProfileUpdatedAt() == null || score.getUserBProfileUpdatedAt() == null) {
            return true;
        }

        Date userAUpdatedAt = getAssessmentProfileUpdatedAt(userA);
        Date userBUpdatedAt = getAssessmentProfileUpdatedAt(userB);

        if (userAUpdatedAt != null && score.getUserAProfileUpdatedAt().before(userAUpdatedAt)) {
            return true;
        }
        if (userBUpdatedAt != null && score.getUserBProfileUpdatedAt().before(userBUpdatedAt)) {
            return true;
        }

        return false;
    }

    private Date getAssessmentProfileUpdatedAt(User user) {
        return assessmentProfileRepo.findByUser(user)
                .map(UserAssessmentProfile::getLastUpdated)
                .orElse(null);
    }

    private Map<String, Object> callAICompatibilityService(User userA, User userB) throws Exception {
        String url = aiServiceUrl + "/match/calculate";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = Map.of(
                "user_a", buildUserProfileForMatching(userA),
                "user_b", buildUserProfileForMatching(userB)
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
        );

        return response.getBody();
    }

    private void calculatePersonalityBasedCompatibility(CompatibilityScore score, User userA, User userB) {
        // Check dealbreakers first
        if (!assessmentService.checkDealbreakers(userA, userB)) {
            score.setPersonalityScore(0.0);
            score.setValuesScore(0.0);
            score.setLifestyleScore(0.0);
            score.setAttractionScore(0.0);
            score.setCircumstantialScore(0.0);
            score.setGrowthScore(0.0);
            score.setOverallScore(0.0);
            return;
        }

        // Try comprehensive AURA assessment first
        UserAssessmentProfile assessmentA = assessmentProfileRepo.findByUser(userA).orElse(null);
        UserAssessmentProfile assessmentB = assessmentProfileRepo.findByUser(userB).orElse(null);

        double personalityScore = 50.0;
        double valuesScore = 50.0;
        double lifestyleScore = 50.0;

        // Calculate Big Five personality compatibility from comprehensive assessment
        if (assessmentA != null && assessmentB != null &&
            Boolean.TRUE.equals(assessmentA.getBigFiveComplete()) &&
            Boolean.TRUE.equals(assessmentB.getBigFiveComplete())) {

            personalityScore = calculateBigFiveCompatibility(assessmentA, assessmentB);

            // Attachment style compatibility bonus
            if (assessmentA.getAttachmentStyle() != null && assessmentB.getAttachmentStyle() != null) {
                double attachmentBonus = calculateAttachmentCompatibility(assessmentA, assessmentB);
                personalityScore = (personalityScore * 0.8) + (attachmentBonus * 0.2);
            }
        } else {
            // Fallback to legacy personality profile
            UserPersonalityProfile profileA = personalityRepo.findByUser(userA).orElse(null);
            UserPersonalityProfile profileB = personalityRepo.findByUser(userB).orElse(null);

            if (profileA != null && profileB != null && profileA.isComplete() && profileB.isComplete()) {
                double diff = 0;
                diff += Math.abs(profileA.getOpenness() - profileB.getOpenness());
                diff += Math.abs(profileA.getConscientiousness() - profileB.getConscientiousness());
                diff += Math.abs(profileA.getExtraversion() - profileB.getExtraversion());
                diff += Math.abs(profileA.getAgreeableness() - profileB.getAgreeableness());
                diff += Math.abs(profileA.getNeuroticism() - profileB.getNeuroticism());
                personalityScore = 100 - (diff / 5);
            }
        }

        // Calculate values compatibility from comprehensive assessment
        if (assessmentA != null && assessmentB != null &&
            Boolean.TRUE.equals(assessmentA.getValuesComplete()) &&
            Boolean.TRUE.equals(assessmentB.getValuesComplete())) {
            valuesScore = calculateValuesCompatibility(assessmentA, assessmentB);
        } else {
            // Fallback to economic values compatibility
            valuesScore = calculateEconomicValuesCompatibility(userA, userB);
        }

        // Calculate lifestyle compatibility from comprehensive assessment
        if (assessmentA != null && assessmentB != null &&
            Boolean.TRUE.equals(assessmentA.getLifestyleComplete()) &&
            Boolean.TRUE.equals(assessmentB.getLifestyleComplete())) {
            lifestyleScore = calculateLifestyleCompatibility(assessmentA, assessmentB);
        }

        score.setPersonalityScore(personalityScore);
        score.setValuesScore(valuesScore);
        score.setLifestyleScore(lifestyleScore);
        score.setAttractionScore(50.0);
        score.setCircumstantialScore(50.0);
        score.setGrowthScore(50.0);

        // Weighted average with all comprehensive scores
        score.setOverallScore(
                personalityScore * 0.25 +
                valuesScore * 0.25 +
                lifestyleScore * 0.2 +
                50.0 * 0.15 + // attraction
                50.0 * 0.1 +  // circumstantial
                50.0 * 0.05   // growth
        );

        // Calculate Enemy % (incompatibility score)
        double enemyScore = calculateEnemyScore(userA, userB, score);
        score.setEnemyScore(enemyScore);
    }

    /**
     * Calculate Enemy % - measures fundamental incompatibilities.
     * High enemy score = more dealbreaker conflicts, opposing values, incompatible lifestyles.
     * This is similar to OKCupid's "Enemy" percentage.
     */
    private double calculateEnemyScore(User userA, User userB, CompatibilityScore compatScore) {
        double enemyScore = 0.0;
        int factors = 0;

        // Factor 1: Dealbreaker violations (most impactful)
        UserAssessmentProfile assessmentA = assessmentProfileRepo.findByUser(userA).orElse(null);
        UserAssessmentProfile assessmentB = assessmentProfileRepo.findByUser(userB).orElse(null);

        if (assessmentA != null && assessmentB != null) {
            // Check for dealbreaker flag conflicts
            double dealbreakerConflict = calculateDealbreakerConflict(assessmentA, assessmentB);
            enemyScore += dealbreakerConflict * 3.0; // Weight heavily
            factors += 3;
        }

        // Factor 2: Value opposition (inverse of values score)
        if (compatScore.getValuesScore() != null) {
            // Values below 50% indicate opposition, above indicates alignment
            double valuesOpposition = Math.max(0, 50 - compatScore.getValuesScore()) * 2;
            enemyScore += valuesOpposition;
            factors++;
        }

        // Factor 3: Political/economic values conflict
        double politicalConflict = calculatePoliticalConflict(userA, userB);
        if (politicalConflict > 0) {
            enemyScore += politicalConflict;
            factors++;
        }

        // Factor 4: Attachment style clash penalty
        if (assessmentA != null && assessmentB != null &&
            assessmentA.getAttachmentStyle() != null && assessmentB.getAttachmentStyle() != null) {
            double attachmentClash = calculateAttachmentClash(assessmentA, assessmentB);
            enemyScore += attachmentClash;
            factors++;
        }

        // Factor 5: Lifestyle incompatibility
        if (compatScore.getLifestyleScore() != null) {
            double lifestyleConflict = Math.max(0, 50 - compatScore.getLifestyleScore()) * 2;
            enemyScore += lifestyleConflict;
            factors++;
        }

        // Normalize to 0-100 scale
        if (factors > 0) {
            enemyScore = Math.min(100, enemyScore / factors);
        }

        return enemyScore;
    }

    private double calculateDealbreakerConflict(UserAssessmentProfile a, UserAssessmentProfile b) {
        if (a.getDealbreakerFlags() == null || b.getDealbreakerFlags() == null) {
            return 0.0;
        }

        Integer flagsA = a.getDealbreakerFlags();
        Integer flagsB = b.getDealbreakerFlags();

        // Use bitwise XOR to find conflicting flags
        // Each bit position represents a different dealbreaker preference
        int xorResult = flagsA ^ flagsB;

        // Count the number of conflicting bits
        int conflictingBits = Integer.bitCount(xorResult);

        // Count total bits set in either profile (active dealbreakers)
        int totalBits = Integer.bitCount(flagsA | flagsB);

        if (totalBits > 0) {
            return (conflictingBits * 100.0) / totalBits;
        }

        return 0.0;
    }

    private double calculatePoliticalConflict(User userA, User userB) {
        var paA = userA.getPoliticalAssessment();
        var paB = userB.getPoliticalAssessment();

        if (paA == null || paB == null) {
            return 0.0;
        }

        double conflict = 0.0;

        // Economic values score difference
        if (paA.getEconomicValuesScore() != null && paB.getEconomicValuesScore() != null) {
            double diff = Math.abs(paA.getEconomicValuesScore() - paB.getEconomicValuesScore());
            conflict += diff; // 0-100 scale
        }

        // Political orientation opposition
        if (paA.getPoliticalOrientation() != null && paB.getPoliticalOrientation() != null) {
            int ordinalA = paA.getPoliticalOrientation().ordinal();
            int ordinalB = paB.getPoliticalOrientation().ordinal();
            int maxDiff = 4; // Assuming 5 orientations (0-4)
            double orientationDiff = Math.abs(ordinalA - ordinalB) * 100.0 / maxDiff;
            conflict = (conflict + orientationDiff) / 2;
        }

        return conflict;
    }

    private double calculateAttachmentClash(UserAssessmentProfile a, UserAssessmentProfile b) {
        UserAssessmentProfile.AttachmentStyle styleA = a.getAttachmentStyle();
        UserAssessmentProfile.AttachmentStyle styleB = b.getAttachmentStyle();

        // Anxious-Avoidant combination has highest conflict potential
        if ((styleA == UserAssessmentProfile.AttachmentStyle.ANXIOUS_PREOCCUPIED &&
             styleB == UserAssessmentProfile.AttachmentStyle.DISMISSIVE_AVOIDANT) ||
            (styleA == UserAssessmentProfile.AttachmentStyle.DISMISSIVE_AVOIDANT &&
             styleB == UserAssessmentProfile.AttachmentStyle.ANXIOUS_PREOCCUPIED)) {
            return 80.0;
        }

        // Fearful-Avoidant with anyone has moderate conflict
        if (styleA == UserAssessmentProfile.AttachmentStyle.FEARFUL_AVOIDANT ||
            styleB == UserAssessmentProfile.AttachmentStyle.FEARFUL_AVOIDANT) {
            return 50.0;
        }

        // Two insecure styles (not anxious-avoidant combo)
        if (styleA != UserAssessmentProfile.AttachmentStyle.SECURE &&
            styleB != UserAssessmentProfile.AttachmentStyle.SECURE) {
            return 30.0;
        }

        // One secure partner stabilizes
        return 0.0;
    }

    private double calculateBigFiveCompatibility(UserAssessmentProfile a, UserAssessmentProfile b) {
        double totalDiff = 0;
        int count = 0;

        if (a.getOpennessScore() != null && b.getOpennessScore() != null) {
            totalDiff += Math.abs(a.getOpennessScore() - b.getOpennessScore());
            count++;
        }
        if (a.getConscientiousnessScore() != null && b.getConscientiousnessScore() != null) {
            totalDiff += Math.abs(a.getConscientiousnessScore() - b.getConscientiousnessScore());
            count++;
        }
        if (a.getExtraversionScore() != null && b.getExtraversionScore() != null) {
            totalDiff += Math.abs(a.getExtraversionScore() - b.getExtraversionScore());
            count++;
        }
        if (a.getAgreeablenessScore() != null && b.getAgreeablenessScore() != null) {
            totalDiff += Math.abs(a.getAgreeablenessScore() - b.getAgreeablenessScore());
            count++;
        }
        if (a.getEmotionalStabilityScore() != null && b.getEmotionalStabilityScore() != null) {
            totalDiff += Math.abs(a.getEmotionalStabilityScore() - b.getEmotionalStabilityScore());
            count++;
        }

        if (count == 0) return 50.0;
        return 100 - (totalDiff / count);
    }

    private double calculateAttachmentCompatibility(UserAssessmentProfile a, UserAssessmentProfile b) {
        UserAssessmentProfile.AttachmentStyle styleA = a.getAttachmentStyle();
        UserAssessmentProfile.AttachmentStyle styleB = b.getAttachmentStyle();

        // Secure-Secure is ideal
        if (styleA == UserAssessmentProfile.AttachmentStyle.SECURE &&
            styleB == UserAssessmentProfile.AttachmentStyle.SECURE) {
            return 100.0;
        }
        // One secure can stabilize another
        if (styleA == UserAssessmentProfile.AttachmentStyle.SECURE ||
            styleB == UserAssessmentProfile.AttachmentStyle.SECURE) {
            return 80.0;
        }
        // Anxious-Avoidant is challenging
        if ((styleA == UserAssessmentProfile.AttachmentStyle.ANXIOUS_PREOCCUPIED &&
             styleB == UserAssessmentProfile.AttachmentStyle.DISMISSIVE_AVOIDANT) ||
            (styleA == UserAssessmentProfile.AttachmentStyle.DISMISSIVE_AVOIDANT &&
             styleB == UserAssessmentProfile.AttachmentStyle.ANXIOUS_PREOCCUPIED)) {
            return 30.0;
        }
        // Same insecure style
        if (styleA == styleB) {
            return 50.0;
        }
        // Default mixed insecure
        return 40.0;
    }

    private double calculateValuesCompatibility(UserAssessmentProfile a, UserAssessmentProfile b) {
        double totalDiff = 0;
        int count = 0;

        if (a.getValuesProgressiveScore() != null && b.getValuesProgressiveScore() != null) {
            totalDiff += Math.abs(a.getValuesProgressiveScore() - b.getValuesProgressiveScore());
            count++;
        }
        if (a.getValuesEgalitarianScore() != null && b.getValuesEgalitarianScore() != null) {
            totalDiff += Math.abs(a.getValuesEgalitarianScore() - b.getValuesEgalitarianScore());
            count++;
        }

        if (count == 0) return 50.0;
        return 100 - (totalDiff / count);
    }

    private double calculateLifestyleCompatibility(UserAssessmentProfile a, UserAssessmentProfile b) {
        double totalDiff = 0;
        int count = 0;

        if (a.getLifestyleSocialScore() != null && b.getLifestyleSocialScore() != null) {
            totalDiff += Math.abs(a.getLifestyleSocialScore() - b.getLifestyleSocialScore());
            count++;
        }
        if (a.getLifestyleHealthScore() != null && b.getLifestyleHealthScore() != null) {
            totalDiff += Math.abs(a.getLifestyleHealthScore() - b.getLifestyleHealthScore());
            count++;
        }
        if (a.getLifestyleWorkLifeScore() != null && b.getLifestyleWorkLifeScore() != null) {
            totalDiff += Math.abs(a.getLifestyleWorkLifeScore() - b.getLifestyleWorkLifeScore());
            count++;
        }
        if (a.getLifestyleFinanceScore() != null && b.getLifestyleFinanceScore() != null) {
            totalDiff += Math.abs(a.getLifestyleFinanceScore() - b.getLifestyleFinanceScore());
            count++;
        }

        if (count == 0) return 50.0;
        return 100 - (totalDiff / count);
    }

    private double calculateEconomicValuesCompatibility(User userA, User userB) {
        // Use PoliticalAssessmentService to get economic compatibility
        return politicalAssessmentService.getEconomicCompatibility(userA, userB);
    }

    private Map<String, Object> buildUserProfileForMatching(User user) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("uuid", user.getUuid().toString());

        // Try comprehensive AURA assessment first
        UserAssessmentProfile assessment = assessmentProfileRepo.findByUser(user).orElse(null);
        if (assessment != null && Boolean.TRUE.equals(assessment.getBigFiveComplete())) {
            Map<String, Object> personalityData = new HashMap<>();
            personalityData.put("openness", assessment.getOpennessScore());
            personalityData.put("conscientiousness", assessment.getConscientiousnessScore());
            personalityData.put("extraversion", assessment.getExtraversionScore());
            personalityData.put("agreeableness", assessment.getAgreeablenessScore());
            personalityData.put("emotionalStability", assessment.getEmotionalStabilityScore());
            personalityData.put("neuroticism", assessment.getNeuroticismScore());
            profile.put("personality", personalityData);

            // Add attachment style if available
            if (Boolean.TRUE.equals(assessment.getAttachmentComplete())) {
                profile.put("attachment", Map.of(
                        "style", assessment.getAttachmentStyle().name(),
                        "anxiety", assessment.getAttachmentAnxietyScore(),
                        "avoidance", assessment.getAttachmentAvoidanceScore()
                ));
            }

            // Add values if complete
            if (Boolean.TRUE.equals(assessment.getValuesComplete())) {
                Map<String, Object> valuesData = new HashMap<>();
                if (assessment.getValuesProgressiveScore() != null) {
                    valuesData.put("progressive", assessment.getValuesProgressiveScore());
                }
                if (assessment.getValuesEgalitarianScore() != null) {
                    valuesData.put("egalitarian", assessment.getValuesEgalitarianScore());
                }
                profile.put("assessmentValues", valuesData);
            }

            // Add lifestyle if complete
            if (Boolean.TRUE.equals(assessment.getLifestyleComplete())) {
                Map<String, Object> lifestyleData = new HashMap<>();
                if (assessment.getLifestyleSocialScore() != null) {
                    lifestyleData.put("social", assessment.getLifestyleSocialScore());
                }
                if (assessment.getLifestyleHealthScore() != null) {
                    lifestyleData.put("health", assessment.getLifestyleHealthScore());
                }
                if (assessment.getLifestyleWorkLifeScore() != null) {
                    lifestyleData.put("workLife", assessment.getLifestyleWorkLifeScore());
                }
                if (assessment.getLifestyleFinanceScore() != null) {
                    lifestyleData.put("finance", assessment.getLifestyleFinanceScore());
                }
                profile.put("lifestyle", lifestyleData);
            }

            // Add dealbreaker flags
            if (Boolean.TRUE.equals(assessment.getDealbreakerComplete()) && assessment.getDealbreakerFlags() != null) {
                profile.put("dealbreakerFlags", assessment.getDealbreakerFlags());
            }
        } else if (user.getPersonalityProfile() != null && user.getPersonalityProfile().isComplete()) {
            // Fallback to legacy personality profile
            UserPersonalityProfile pp = user.getPersonalityProfile();
            profile.put("personality", Map.of(
                    "openness", pp.getOpenness(),
                    "conscientiousness", pp.getConscientiousness(),
                    "extraversion", pp.getExtraversion(),
                    "agreeableness", pp.getAgreeableness(),
                    "neuroticism", pp.getNeuroticism()
            ));
        }

        // Add economic values for AI matching
        if (user.getPoliticalAssessment() != null) {
            var pa = user.getPoliticalAssessment();
            Map<String, Object> economicValues = new HashMap<>();
            if (pa.getEconomicClass() != null) {
                economicValues.put("economicClass", pa.getEconomicClass().name());
            }
            if (pa.getPoliticalOrientation() != null) {
                economicValues.put("politicalOrientation", pa.getPoliticalOrientation().name());
            }
            if (pa.getEconomicValuesScore() != null) {
                economicValues.put("economicValuesScore", pa.getEconomicValuesScore());
            }
            if (pa.getClassConsciousnessScore() != null) {
                economicValues.put("classConsciousnessScore", pa.getClassConsciousnessScore());
            }
            profile.put("economicValues", economicValues);
        }

        if (user.getInterests() != null) {
            profile.put("interests", user.getInterests().stream()
                    .map(i -> i.getText())
                    .collect(Collectors.toList()));
        }

        profile.put("age", getAge(user));
        profile.put("gender", user.getGender() != null ? user.getGender().getId() : null);

        return profile;
    }

    private int getAge(User user) {
        if (user.getDates() != null && user.getDates().getDateOfBirth() != null) {
            LocalDate dob = user.getDates().getDateOfBirth().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            return LocalDate.now().getYear() - dob.getYear();
        }
        return 0;
    }

    private UserDailyMatchLimit createNewDailyLimit(User user, Date today) {
        UserDailyMatchLimit limit = new UserDailyMatchLimit();
        limit.setUser(user);
        limit.setMatchDate(today);
        limit.setMatchesShown(0);
        limit.setMatchLimit(dailyMatchLimit);
        return matchLimitRepo.save(limit);
    }

    private Date truncateToDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private Date getNextMidnight() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private List<MatchRecommendationDto> convertToMatchRecommendations(List<Map<String, Object>> rawList) {
        try {
            User currentUser = authService.getCurrentUser(true);

            return rawList.stream().map(item -> {
                MatchRecommendationDto dto = new MatchRecommendationDto();
                Long userId = ((Number) item.get("user_id")).longValue();
                dto.setUserId(userId);
                dto.setUserUuid((String) item.get("uuid"));
                dto.setCompatibilityScore(((Number) item.get("compatibility_score")).doubleValue());

                // Try to populate OKCupid-style data if possible
                try {
                    User matchUser = userRepo.findById(userId).orElse(null);
                    if (matchUser != null && currentUser != null) {
                        populateOkCupidMatchData(dto, currentUser, matchUser);
                    }
                } catch (Exception e) {
                    LOGGER.debug("Could not populate OKCupid data for user {}", userId, e);
                }

                return dto;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.error("Failed to convert match recommendations", e);
            return Collections.emptyList();
        }
    }

    private void updateShownUserIds(UserDailyMatchLimit limit, List<MatchRecommendationDto> matches) {
        try {
            List<Long> existingIds = new ArrayList<>();
            if (limit.getShownUserIds() != null) {
                existingIds = objectMapper.readValue(limit.getShownUserIds(),
                        new TypeReference<List<Long>>() {});
            }
            for (MatchRecommendationDto match : matches) {
                existingIds.add(match.getUserId());
            }
            limit.setShownUserIds(objectMapper.writeValueAsString(existingIds));
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to update shown user IDs", e);
        }
    }

    /**
     * Populate OKCupid-style match data into the MatchRecommendationDto.
     * This is the "Marriage Machine" feature that made OKCupid/eHarmony successful.
     *
     * Uses:
     * - Importance-weighted matching (user decides what matters)
     * - Acceptable answers (what partner answers are OK)
     * - Geometric mean formula: sqrt(your_satisfaction * their_satisfaction) * 100
     * - Category breakdown for transparency
     * - Mandatory conflict detection (dealbreakers)
     */
    @SuppressWarnings("unchecked")
    private void populateOkCupidMatchData(MatchRecommendationDto dto, User currentUser, User matchUser) {
        try {
            // Get OKCupid-style match percentage with importance weighting
            Map<String, Object> matchData = assessmentService.calculateOkCupidMatch(currentUser, matchUser);

            // Set match percentage (kept for internal use, deprecated for display)
            Double matchPercentage = (Double) matchData.get("matchPercentage");
            dto.setMatchPercentage(matchPercentage != null ? matchPercentage : 50.0);

            // Set human-readable match category (for user-facing display)
            dto.setMatchCategory(scoreToCategory(matchPercentage));

            // Set common questions count (reliability indicator)
            Integer commonQuestions = (Integer) matchData.get("commonQuestions");
            dto.setCommonQuestionsCount(commonQuestions != null ? commonQuestions : 0);

            // Set mandatory conflict flag (dealbreaker detected)
            Boolean hasMandatoryConflict = (Boolean) matchData.get("hasMandatoryConflict");
            dto.setHasMandatoryConflict(hasMandatoryConflict != null ? hasMandatoryConflict : false);

            // Get detailed explanation with category breakdown
            Map<String, Object> explanation = assessmentService.getMatchExplanation(currentUser, matchUser);

            // Set category breakdown (deprecated numerical values kept for internal use)
            Map<String, Double> categoryBreakdown = (Map<String, Double>) explanation.get("categoryBreakdown");
            if (categoryBreakdown != null && !categoryBreakdown.isEmpty()) {
                dto.setCategoryBreakdown(categoryBreakdown);

                // Generate category labels (non-numerical) for user display
                dto.setCategoryLabels(generateCategoryLabels(categoryBreakdown));

                // Generate top compatibility areas using labels instead of percentages
                dto.setTopCompatibilityAreas(generateTopCompatibilityAreas(categoryBreakdown));

                // Generate areas to discuss (moderate mismatches)
                dto.setAreasToDiscuss(generateAreasToDiscuss(categoryBreakdown));
            }

            // Generate match insight summary
            dto.setMatchInsight(generateMatchInsight(dto, categoryBreakdown));

        } catch (Exception e) {
            LOGGER.warn("Failed to populate OKCupid match data, using defaults", e);
            // Set safe defaults
            dto.setMatchPercentage(dto.getCompatibilityScore() != null ? dto.getCompatibilityScore() : 50.0);
            dto.setMatchCategory(scoreToCategory(dto.getCompatibilityScore()));
            dto.setCommonQuestionsCount(0);
            dto.setHasMandatoryConflict(false);
        }
    }

    /**
     * Convert a numerical score to a human-readable match category.
     * Prevents optimization/gaming behavior by hiding precise numbers.
     *
     * @param score The numerical score (0-100)
     * @return Human-readable category label
     */
    private String scoreToCategory(Double score) {
        if (score == null) return "Exploring Match";
        if (score >= 90) return "Exceptional Match";
        if (score >= 75) return "Strong Match";
        if (score >= 60) return "Good Match";
        if (score >= 45) return "Fair Match";
        return "Exploring Match";
    }

    /**
     * Convert a numerical dimension score to a human-readable label.
     *
     * @param score The numerical score (0-100)
     * @return Human-readable dimension label
     */
    private String scoreToDimensionLabel(Double score) {
        if (score == null) return "Developing";
        if (score >= 80) return "Excellent";
        if (score >= 65) return "Good";
        if (score >= 50) return "Fair";
        return "Developing";
    }

    /**
     * Generate category labels map (non-numerical) from category breakdown.
     *
     * @param categoryBreakdown The numerical category scores
     * @return Map of category names to human-readable labels
     */
    private Map<String, String> generateCategoryLabels(Map<String, Double> categoryBreakdown) {
        Map<String, String> labels = new HashMap<>();
        if (categoryBreakdown == null) return labels;

        for (Map.Entry<String, Double> entry : categoryBreakdown.entrySet()) {
            String category = formatCategoryName(entry.getKey());
            String label = scoreToDimensionLabel(entry.getValue());
            labels.put(category, label);
        }

        return labels;
    }

    /**
     * Generate top 3 compatibility areas from category scores.
     * Shows users WHERE they're compatible using labels instead of percentages.
     */
    private List<String> generateTopCompatibilityAreas(Map<String, Double> categoryBreakdown) {
        List<String> topAreas = new ArrayList<>();

        // Sort categories by score descending
        List<Map.Entry<String, Double>> sorted = categoryBreakdown.entrySet().stream()
                .filter(e -> e.getValue() >= 70) // Only show high-compatibility areas
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(3)
                .collect(Collectors.toList());

        for (Map.Entry<String, Double> entry : sorted) {
            String category = formatCategoryName(entry.getKey());
            String label = scoreToDimensionLabel(entry.getValue());
            // Use label instead of percentage
            topAreas.add(category + ": " + label);
        }

        if (topAreas.isEmpty()) {
            topAreas.add("You have unique qualities that complement each other");
        }

        return topAreas;
    }

    /**
     * Generate areas that need discussion (moderate mismatches).
     * These aren't dealbreakers but worth talking about.
     */
    private List<String> generateAreasToDiscuss(Map<String, Double> categoryBreakdown) {
        List<String> areasToDiscuss = new ArrayList<>();

        for (Map.Entry<String, Double> entry : categoryBreakdown.entrySet()) {
            double score = entry.getValue();
            // Areas between 40-60% are worth discussing
            if (score >= 40 && score < 60) {
                String area = getDiscussionPoint(entry.getKey(), score);
                if (area != null) {
                    areasToDiscuss.add(area);
                }
            }
        }

        // Limit to top 3 discussion points
        return areasToDiscuss.stream().limit(3).collect(Collectors.toList());
    }

    private String getDiscussionPoint(String category, double score) {
        return switch (category.toUpperCase()) {
            case "BIG_FIVE" -> "Different personality styles - could complement each other";
            case "ATTACHMENT" -> "Different attachment needs - worth discussing expectations";
            case "VALUES" -> "Some different perspectives on values - explore through conversation";
            case "LIFESTYLE" -> "Different lifestyle preferences - find common ground";
            case "DEALBREAKER" -> "Some preferences differ - clarify what matters most";
            default -> null;
        };
    }

    private String formatCategoryName(String category) {
        return switch (category.toUpperCase()) {
            case "BIG_FIVE" -> "Personality";
            case "ATTACHMENT" -> "Attachment style";
            case "VALUES" -> "Core values";
            case "LIFESTYLE" -> "Lifestyle";
            case "DEALBREAKER" -> "Compatibility";
            default -> category.substring(0, 1).toUpperCase() +
                       category.substring(1).toLowerCase().replace("_", " ");
        };
    }

    /**
     * Generate a brief insight about the match.
     * This helps users understand WHY they match, not just a percentage.
     */
    private String generateMatchInsight(MatchRecommendationDto dto, Map<String, Double> categoryBreakdown) {
        Double matchPercentage = dto.getMatchPercentage();

        if (Boolean.TRUE.equals(dto.getHasMandatoryConflict())) {
            return "⚠️ Potential dealbreaker detected - review compatibility carefully";
        }

        if (matchPercentage == null) {
            return "Answer more questions to improve match accuracy";
        }

        if (dto.getCommonQuestionsCount() != null && dto.getCommonQuestionsCount() < 10) {
            return "Based on " + dto.getCommonQuestionsCount() + " shared questions - answer more for better accuracy";
        }

        if (matchPercentage >= 90) {
            return "Exceptional compatibility! Strong alignment across all areas";
        } else if (matchPercentage >= 80) {
            return "High compatibility - you share important values and perspectives";
        } else if (matchPercentage >= 70) {
            return "Good compatibility - similar outlook with room for discovery";
        } else if (matchPercentage >= 60) {
            return "Moderate compatibility - some differences could add variety";
        } else {
            return "Different perspectives - could challenge each other in healthy ways";
        }
    }
}
