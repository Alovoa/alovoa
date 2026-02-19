package com.nonononoki.alovoa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.Conversation;
import com.nonononoki.alovoa.entity.user.Message;
import com.nonononoki.alovoa.entity.user.ReportEvidence;
import com.nonononoki.alovoa.entity.user.ReportEvidence.EvidenceType;
import com.nonononoki.alovoa.entity.user.ReportEvidence.VerificationMethod;
import com.nonononoki.alovoa.entity.user.UserAccountabilityReport;
import com.nonononoki.alovoa.entity.user.UserAccountabilityReport.*;
import com.nonononoki.alovoa.entity.user.UserBehaviorEvent;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.MessageRepository;
import com.nonononoki.alovoa.repo.ReportEvidenceRepository;
import com.nonononoki.alovoa.repo.UserAccountabilityReportRepository;
import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.service.ml.JavaMediaBackendService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for Public Accountability System.
 * Handles report submission, evidence verification, and public feedback display.
 */
@Service
public class AccountabilityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountabilityService.class);

    // Rate limits
    private static final int MAX_REPORTS_PER_DAY = 3;
    private static final int MAX_REPORTS_PER_SUBJECT_PER_MONTH = 1;
    private static final double MIN_VERIFICATION_CONFIDENCE = 70.0;

    @Autowired
    private UserAccountabilityReportRepository reportRepo;

    @Autowired
    private ReportEvidenceRepository evidenceRepo;

    @Autowired
    private ConversationRepository conversationRepo;

    @Autowired
    private MessageRepository messageRepo;

    @Autowired
    private ReputationService reputationService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JavaMediaBackendService javaMediaBackendService;

    @Value("${app.aura.media-service.url:http://localhost:8001}")
    private String mediaServiceUrl;

    /**
     * Submit a new accountability report
     */
    @Transactional
    public UserAccountabilityReport submitReport(
            User reporter,
            User subject,
            AccountabilityCategory category,
            BehaviorType behaviorType,
            String title,
            String description,
            boolean anonymous,
            Long conversationId) throws Exception {

        // Validation
        if (reporter.equals(subject)) {
            throw new IllegalArgumentException("Cannot report yourself");
        }

        // Rate limiting - max reports per day
        Date dayAgo = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
        long recentReports = reportRepo.countRecentReportsByReporter(reporter, dayAgo);
        if (recentReports >= MAX_REPORTS_PER_DAY) {
            throw new IllegalStateException("You have reached the daily report limit");
        }

        // Check for duplicate report
        Date monthAgo = Date.from(Instant.now().minus(30, ChronoUnit.DAYS));
        Optional<UserAccountabilityReport> existingReport =
            reportRepo.findByReporterAndSubjectAndCategory(reporter, subject, category);
        if (existingReport.isPresent() &&
            existingReport.get().getCreatedAt().after(monthAgo)) {
            throw new IllegalStateException("You have already reported this user for this category recently");
        }

        // Create report
        UserAccountabilityReport report = new UserAccountabilityReport();
        report.setUuid(UUID.randomUUID());
        report.setReporter(reporter);
        report.setSubject(subject);
        report.setCategory(category);
        report.setBehaviorType(behaviorType);
        report.setTitle(title);
        report.setDescription(description);
        report.setAnonymous(anonymous);
        report.setCreatedAt(new Date());
        report.setStatus(ReportStatus.PENDING_VERIFICATION);
        report.setVisibility(ReportVisibility.HIDDEN);

        // If from a conversation, link it for verification
        if (conversationId != null) {
            Optional<Conversation> conversation = conversationRepo.findById(conversationId);
            if (conversation.isPresent()) {
                boolean isParticipant = conversation.get().getUsers().contains(reporter);
                if (isParticipant) {
                    report.setFromMatch(true);
                    report.setConversationId(conversationId);
                }
            }
        }

        // Calculate reputation impact based on category
        report.setReputationImpact(calculateReputationImpact(category));

        return reportRepo.save(report);
    }

    /**
     * Upload evidence for a report
     */
    @Transactional
    public ReportEvidence uploadEvidence(
            UUID reportUuid,
            MultipartFile file,
            EvidenceType evidenceType,
            String caption,
            User uploader) throws Exception {

        UserAccountabilityReport report = reportRepo.findByUuid(reportUuid)
            .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        // Verify uploader is the reporter
        if (!report.getReporter().equals(uploader)) {
            throw new IllegalAccessException("Only the reporter can add evidence");
        }

        // Check evidence limit
        long existingCount = evidenceRepo.countByReport(report);
        if (existingCount >= 5) {
            throw new IllegalStateException("Maximum evidence items reached");
        }

        // Upload file to media service
        String fileUrl = uploadToMediaService(file);

        // Create evidence record
        ReportEvidence evidence = new ReportEvidence();
        evidence.setUuid(UUID.randomUUID());
        evidence.setReport(report);
        evidence.setEvidenceType(evidenceType);
        evidence.setFileUrl(fileUrl);
        evidence.setOriginalFilename(file.getOriginalFilename());
        evidence.setMimeType(file.getContentType());
        evidence.setFileSize(file.getSize());
        evidence.setCaption(caption);
        evidence.setDisplayOrder((int) existingCount);
        evidence.setUploadedAt(new Date());

        // Generate image hash for duplicate detection
        evidence.setImageHash(generateImageHash(file.getBytes()));

        // Check for duplicate evidence
        Optional<ReportEvidence> duplicate = evidenceRepo.findByImageHash(evidence.getImageHash());
        if (duplicate.isPresent()) {
            LOGGER.warn("Duplicate evidence detected for report {}", reportUuid);
        }

        return evidenceRepo.save(evidence);
    }

    /**
     * Verify screenshot evidence against message database
     */
    @Transactional
    public void verifyScreenshotEvidence(ReportEvidence evidence) {
        if (evidence.getEvidenceType() != EvidenceType.SCREENSHOT_MESSAGE) {
            evidence.setVerificationMethod(VerificationMethod.NONE);
            evidenceRepo.save(evidence);
            return;
        }

        UserAccountabilityReport report = evidence.getReport();

        // Only verify if report is from a match
        if (!report.isFromMatch() || report.getConversationId() == null) {
            evidence.setVerificationMethod(VerificationMethod.NONE);
            evidenceRepo.save(evidence);
            return;
        }

        try {
            // Call AI service for OCR
            String extractedText = performOcr(evidence.getFileUrl());
            evidence.setExtractedText(extractedText);

            if (extractedText == null || extractedText.isBlank()) {
                evidence.setVerified(false);
                evidence.setVerificationMethod(VerificationMethod.NONE);
                evidenceRepo.save(evidence);
                return;
            }

            // Search for matching messages in the conversation
            Optional<Conversation> conversation = conversationRepo.findById(report.getConversationId());
            if (conversation.isEmpty()) {
                evidence.setVerified(false);
                evidenceRepo.save(evidence);
                return;
            }

            List<Message> messages = conversation.get().getMessages();
            List<Long> matchedIds = new ArrayList<>();
            double totalConfidence = 0;
            int matchCount = 0;

            for (Message msg : messages) {
                if (msg.getContent() != null) {
                    double similarity = calculateTextSimilarity(extractedText, msg.getContent());
                    if (similarity > 0.7) {
                        matchedIds.add(msg.getId());
                        totalConfidence += similarity;
                        matchCount++;
                    }
                }
            }

            if (matchCount > 0) {
                evidence.setVerified(true);
                evidence.setVerificationMethod(VerificationMethod.OCR_MESSAGE_MATCH);
                evidence.setVerificationConfidence((totalConfidence / matchCount) * 100);
                evidence.setMatchedMessageIds(matchedIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));
                evidence.setVerifiedAt(new Date());
            } else {
                evidence.setVerified(false);
                evidence.setVerificationMethod(VerificationMethod.OCR_MESSAGE_MATCH);
                evidence.setVerificationConfidence(0.0);
            }

            evidenceRepo.save(evidence);

        } catch (Exception e) {
            LOGGER.error("Failed to verify screenshot evidence", e);
            evidence.setVerificationMethod(VerificationMethod.NONE);
            evidenceRepo.save(evidence);
        }
    }

    /**
     * Admin/system verification of a report
     */
    @Transactional
    public void verifyReport(UUID reportUuid, boolean verified, String notes) {
        UserAccountabilityReport report = reportRepo.findByUuid(reportUuid)
            .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        report.setVerificationNotes(notes);

        if (verified) {
            // Check if evidence supports verification
            List<ReportEvidence> evidenceList = evidenceRepo.findByReportOrderByDisplayOrderAsc(report);
            boolean hasVerifiedEvidence = evidenceList.stream()
                .anyMatch(e -> e.isVerified() &&
                              e.getVerificationConfidence() != null &&
                              e.getVerificationConfidence() >= MIN_VERIFICATION_CONFIDENCE);

            report.setEvidenceVerified(hasVerifiedEvidence);
            report.setStatus(ReportStatus.VERIFIED);
            report.setVerifiedAt(new Date());

        } else {
            report.setStatus(ReportStatus.EVIDENCE_INSUFFICIENT);
        }

        reportRepo.save(report);
    }

    /**
     * Publish a verified report (make it visible on profile)
     */
    @Transactional
    public void publishReport(UUID reportUuid, ReportVisibility visibility) {
        UserAccountabilityReport report = reportRepo.findByUuid(reportUuid)
            .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        if (report.getStatus() != ReportStatus.VERIFIED) {
            throw new IllegalStateException("Only verified reports can be published");
        }

        report.setStatus(ReportStatus.PUBLISHED);
        report.setVisibility(visibility);
        report.setPublishedAt(new Date());
        reportRepo.save(report);

        // Impact reputation of the subject
        if (report.getReputationImpact() != null) {
            UserBehaviorEvent.BehaviorType behaviorType = mapCategoryToBehaviorType(report.getCategory());
            reputationService.recordBehavior(
                report.getSubject(),
                behaviorType,
                report.getReporter(),
                Map.of("reportId", report.getId(), "category", report.getCategory().name())
            );
        }
    }

    /**
     * Subject responds to a report
     */
    @Transactional
    public void submitSubjectResponse(UUID reportUuid, User subject, String response) {
        UserAccountabilityReport report = reportRepo.findByUuid(reportUuid)
            .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        if (!report.getSubject().equals(subject)) {
            throw new IllegalArgumentException("You are not the subject of this report");
        }

        if (report.getSubjectResponse() != null) {
            throw new IllegalStateException("Response already submitted");
        }

        report.setSubjectResponse(response);
        report.setSubjectResponseDate(new Date());
        reportRepo.save(report);
    }

    /**
     * Get public feedback for a user's profile
     */
    public List<UserAccountabilityReport> getPublicFeedback(User subject, User viewer) {
        List<UserAccountabilityReport> allReports =
            reportRepo.findBySubjectAndStatus(subject, ReportStatus.PUBLISHED);

        // Filter based on visibility and viewer relationship
        return allReports.stream()
            .filter(r -> canViewReport(r, viewer, subject))
            .collect(Collectors.toList());
    }

    /**
     * Get feedback summary for profile display
     */
    public Map<String, Object> getFeedbackSummary(User subject) {
        Map<String, Object> summary = new HashMap<>();

        List<Object[]> counts = reportRepo.getReportCountsByCategory(subject);
        int positive = 0;
        int negative = 0;
        Map<String, Integer> byCategory = new HashMap<>();

        for (Object[] row : counts) {
            AccountabilityCategory category = (AccountabilityCategory) row[0];
            int count = ((Long) row[1]).intValue();
            byCategory.put(category.name(), count);

            if (category == AccountabilityCategory.POSITIVE_EXPERIENCE) {
                positive = count;
            } else {
                negative += count;
            }
        }

        summary.put("totalReports", positive + negative);
        summary.put("positiveCount", positive);
        summary.put("negativeCount", negative);
        summary.put("byCategory", byCategory);

        // Calculate feedback score
        int total = positive + negative;
        double score = total > 0 ? (double) positive / total * 100 : 50.0;
        summary.put("feedbackScore", Math.round(score));

        return summary;
    }

    /**
     * Mark report as helpful
     */
    @Transactional
    public void markHelpful(UUID reportUuid, User user) {
        UserAccountabilityReport report = reportRepo.findByUuid(reportUuid)
            .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        report.setHelpfulCount(report.getHelpfulCount() + 1);
        reportRepo.save(report);
    }

    /**
     * Flag report as potentially false
     */
    @Transactional
    public void flagReport(UUID reportUuid, User user) {
        UserAccountabilityReport report = reportRepo.findByUuid(reportUuid)
            .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        report.setFlaggedCount(report.getFlaggedCount() + 1);

        // If heavily flagged, put under review
        if (report.getFlaggedCount() >= 5) {
            report.setStatus(ReportStatus.DISPUTED);
        }

        reportRepo.save(report);

        // If reporter is repeatedly flagged, track their behavior
        Date monthAgo = Date.from(Instant.now().minus(30, ChronoUnit.DAYS));
        List<UserAccountabilityReport> reporterReports =
            reportRepo.findByReporter(report.getReporter());

        long flaggedReports = reporterReports.stream()
            .filter(r -> r.getFlaggedCount() >= 3)
            .count();

        if (flaggedReports >= 3) {
            // Reporter may be making false reports - impact their reputation
            reputationService.recordBehavior(
                report.getReporter(),
                UserBehaviorEvent.BehaviorType.MISREPRESENTATION,
                null,
                Map.of("reason", "Multiple flagged accountability reports")
            );
        }
    }

    /**
     * Retract a report (reporter withdraws)
     */
    @Transactional
    public void retractReport(UUID reportUuid, User reporter) {
        UserAccountabilityReport report = reportRepo.findByUuid(reportUuid)
            .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        if (!report.getReporter().equals(reporter)) {
            throw new IllegalArgumentException("Only the reporter can retract");
        }

        if (report.getStatus() == ReportStatus.PUBLISHED) {
            throw new IllegalStateException("Cannot retract published reports");
        }

        report.setStatus(ReportStatus.RETRACTED);
        reportRepo.save(report);
    }

    /**
     * Get reports pending verification (for admin)
     */
    public Page<UserAccountabilityReport> getPendingReports(int page, int size) {
        return reportRepo.findByStatus(
            ReportStatus.PENDING_VERIFICATION,
            PageRequest.of(page, size)
        );
    }

    /**
     * Detect patterns in reports (multiple reports against same user).
     * Returns users who have 3+ verified reports against them.
     */
    public List<User> detectReportPatterns() {
        List<Object[]> frequentlyReported =
            reportRepo.findFrequentlyReportedUsers(ReportStatus.VERIFIED, 3);

        if (frequentlyReported.isEmpty()) {
            return Collections.emptyList();
        }

        // Extract user IDs from results (each row is [userId, reportCount])
        List<Long> userIds = frequentlyReported.stream()
            .map(row -> (Long) row[0])
            .collect(Collectors.toList());

        // Fetch the users
        List<User> flaggedUsers = userRepo.findAllById(userIds);

        // Log the findings for admin review
        for (Object[] row : frequentlyReported) {
            Long userId = (Long) row[0];
            Long reportCount = (Long) row[1];
            LOGGER.warn("User {} has {} verified reports against them", userId, reportCount);
        }

        return flaggedUsers;
    }

    /**
     * Get report pattern details for a specific user
     */
    public Map<String, Object> getReportPatternDetails(User user) {
        Map<String, Object> details = new HashMap<>();

        List<UserAccountabilityReport> reports = reportRepo.findBySubjectAndStatus(user, ReportStatus.VERIFIED);
        details.put("totalVerifiedReports", reports.size());

        // Group by category
        Map<AccountabilityCategory, Long> byCategory = reports.stream()
            .collect(Collectors.groupingBy(UserAccountabilityReport::getCategory, Collectors.counting()));
        details.put("byCategory", byCategory);

        // Check for escalation patterns (multiple reports in short time)
        Date weekAgo = Date.from(Instant.now().minus(7, ChronoUnit.DAYS));
        long recentReports = reports.stream()
            .filter(r -> r.getCreatedAt().after(weekAgo))
            .count();
        details.put("reportsLastWeek", recentReports);

        // Identify if pattern warrants action
        boolean warrantsBan = reports.size() >= 5 ||
            byCategory.getOrDefault(AccountabilityCategory.HARASSMENT, 0L) >= 2 ||
            byCategory.getOrDefault(AccountabilityCategory.MANIPULATION, 0L) >= 2;
        details.put("warrantsBan", warrantsBan);

        return details;
    }

    // === Helper Methods ===

    private boolean canViewReport(UserAccountabilityReport report, User viewer, User subject) {
        if (viewer == null) return false;
        if (report.getVisibility() == ReportVisibility.HIDDEN) return false;
        if (report.getVisibility() == ReportVisibility.PUBLIC) return true;

        // For MATCHES_ONLY visibility, check if viewer and subject have a conversation
        if (report.getVisibility() == ReportVisibility.MATCHES_ONLY) {
            // Get conversations for both users and check for overlap
            List<Conversation> viewerConversations = conversationRepo.findByUsers_Id(viewer.getId());
            for (Conversation conv : viewerConversations) {
                if (conv.containsUser(subject)) {
                    return true; // They are matched (have a conversation)
                }
            }
            return false; // Not matched
        }

        return false;
    }

    private double calculateReputationImpact(AccountabilityCategory category) {
        return switch (category) {
            case GHOSTING -> -2.0;
            case DISHONESTY -> -5.0;
            case DISRESPECT -> -3.0;
            case HARASSMENT -> -10.0;
            case MANIPULATION -> -8.0;
            case BOUNDARY_VIOLATION -> -4.0;
            case DATE_NO_SHOW -> -5.0;
            case POSITIVE_EXPERIENCE -> 3.0;
        };
    }

    private UserBehaviorEvent.BehaviorType mapCategoryToBehaviorType(AccountabilityCategory category) {
        return switch (category) {
            case GHOSTING -> UserBehaviorEvent.BehaviorType.GHOSTING;
            case DISHONESTY -> UserBehaviorEvent.BehaviorType.MISREPRESENTATION;
            case DISRESPECT -> UserBehaviorEvent.BehaviorType.INAPPROPRIATE_CONTENT;
            case HARASSMENT -> UserBehaviorEvent.BehaviorType.REPORT_UPHELD;
            case MANIPULATION -> UserBehaviorEvent.BehaviorType.REPORT_UPHELD;
            case BOUNDARY_VIOLATION -> UserBehaviorEvent.BehaviorType.REPORT_UPHELD;
            case DATE_NO_SHOW -> UserBehaviorEvent.BehaviorType.NO_SHOW;
            case POSITIVE_EXPERIENCE -> UserBehaviorEvent.BehaviorType.POSITIVE_FEEDBACK;
        };
    }

    private String uploadToMediaService(MultipartFile file) throws Exception {
        if (javaMediaBackendService.isEnabled()) {
            String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
            return javaMediaBackendService.uploadBinary(file.getBytes(), contentType, "evidence", "evidence", true);
        }

        // Create multipart request to media service
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Build multipart body
        org.springframework.util.LinkedMultiValueMap<String, Object> body =
            new org.springframework.util.LinkedMultiValueMap<>();

        // Create a resource from the file bytes
        org.springframework.core.io.ByteArrayResource resource =
            new org.springframework.core.io.ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

        body.add("file", resource);
        body.add("type", "evidence");

        HttpEntity<org.springframework.util.LinkedMultiValueMap<String, Object>> requestEntity =
            new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
            mediaServiceUrl + "/upload",
            HttpMethod.POST,
            requestEntity,
            Map.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            String fileUrl = (String) response.getBody().get("url");
            if (fileUrl != null && !fileUrl.isBlank()) {
                return fileUrl;
            }
        }

        throw new IllegalStateException("Media service upload failed to return a file URL");
    }

    private String performOcr(String imageUrl) {
        if (javaMediaBackendService.isEnabled()) {
            return javaMediaBackendService.ocrFromUrl(imageUrl);
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> request = Map.of("image_url", imageUrl);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                mediaServiceUrl + "/ocr",
                HttpMethod.POST,
                entity,
                Map.class
            );

            if (response.getBody() != null) {
                return (String) response.getBody().get("text");
            }
        } catch (Exception e) {
            LOGGER.error("OCR failed for {}", imageUrl, e);
        }
        return null;
    }

    private double calculateTextSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) return 0;

        // Simple word overlap similarity
        Set<String> words1 = new HashSet<>(Arrays.asList(text1.toLowerCase().split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(text2.toLowerCase().split("\\s+")));

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }

    private String generateImageHash(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }

    // === Appeal-Related Methods ===

    /**
     * Create an appeal for a specific report.
     * Called from ReputationService when a user appeals a report.
     *
     * @param report The report being appealed
     * @param reason The user's reason for the appeal
     * @return true if appeal was created successfully
     */
    @Transactional
    public boolean markReportAsAppealed(UserAccountabilityReport report, String reason) {
        if (report.getStatus() == ReportStatus.REMOVED ||
            report.getStatus() == ReportStatus.RETRACTED) {
            return false; // Can't appeal removed/retracted reports
        }

        // Mark report as disputed while appeal is pending
        if (report.getStatus() == ReportStatus.PUBLISHED) {
            report.setStatus(ReportStatus.DISPUTED);
            reportRepo.save(report);
        }

        return true;
    }

    /**
     * Reverse the reputation impact when an appeal is approved.
     * This undoes the negative reputation effects of the report.
     *
     * @param report The report whose impact should be reversed
     * @param user The user whose reputation should be restored
     */
    @Transactional
    public void reverseReportReputationImpact(UserAccountabilityReport report, User user) {
        if (report.getReputationImpact() == null || report.getReputationImpact() == 0) {
            return;
        }

        // Get the inverse of the original impact
        double reverseImpact = -report.getReputationImpact();

        // Record a behavior event to track the reversal
        UserBehaviorEvent.BehaviorType behaviorType = UserBehaviorEvent.BehaviorType.POSITIVE_FEEDBACK;

        reputationService.recordBehavior(
            user,
            behaviorType,
            null,
            Map.of(
                "reason", "Appeal approved - reputation impact reversed",
                "originalReportId", report.getId(),
                "originalCategory", report.getCategory().name(),
                "reversedImpact", reverseImpact
            )
        );

        LOGGER.info("Reversed reputation impact for user {} from report {}",
                    user.getId(), report.getId());
    }

    /**
     * Handle report outcome after appeal decision.
     * Called when an admin approves an appeal linked to a report.
     *
     * @param reportUuid The UUID of the report
     * @param removeReport Whether to fully remove the report
     */
    @Transactional
    public void handleAppealApprovalForReport(UUID reportUuid, boolean removeReport) {
        UserAccountabilityReport report = reportRepo.findByUuid(reportUuid)
            .orElse(null);

        if (report == null) {
            LOGGER.warn("Report not found for appeal approval: {}", reportUuid);
            return;
        }

        if (removeReport) {
            // Fully remove the report
            report.setStatus(ReportStatus.REMOVED);
            report.setVisibility(ReportVisibility.HIDDEN);
        } else {
            // Keep report but mark as disputed/resolved
            report.setStatus(ReportStatus.DISPUTED);
        }

        reportRepo.save(report);

        // Reverse reputation impact
        reverseReportReputationImpact(report, report.getSubject());
    }

    /**
     * Get reports that a user can appeal (published reports against them).
     *
     * @param user The user (subject of reports)
     * @return List of reports eligible for appeal
     */
    public List<UserAccountabilityReport> getAppealableReports(User user) {
        List<UserAccountabilityReport> allReports = reportRepo.findBySubjectAndStatus(user, ReportStatus.PUBLISHED);

        // Filter to only include reports that are appealable
        // (published and not already disputed)
        return allReports.stream()
            .filter(r -> r.getStatus() == ReportStatus.PUBLISHED ||
                        r.getStatus() == ReportStatus.VERIFIED)
            .collect(Collectors.toList());
    }
}
