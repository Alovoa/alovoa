package com.nonononoki.alovoa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.entity.user.Conversation;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.VideoDate;
import com.nonononoki.alovoa.entity.VideoDate.DateStatus;
import com.nonononoki.alovoa.entity.user.UserBehaviorEvent;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.VideoDateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VideoDateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VideoDateService.class);

    @Value("${app.aura.video-date.max-duration:3600}")
    private Integer maxDurationSeconds;

    @Value("${app.aura.video-date.proposal-expiry-hours:48}")
    private Integer proposalExpiryHours;

    @Value("${app.aura.video-date.room-base-url:https://meet.aura.app}")
    private String roomBaseUrl;

    @Autowired
    private AuthService authService;

    @Autowired
    private VideoDateRepository videoDateRepo;

    @Autowired
    private ConversationRepository conversationRepo;

    @Autowired
    private com.nonononoki.alovoa.repo.UserRepository userRepo;

    @Autowired
    private ReputationService reputationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DonationService donationService;

    public Map<String, Object> proposeVideoDate(Long conversationId, Date proposedTime) throws Exception {
        User user = authService.getCurrentUser(true);

        Conversation conversation = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new Exception("Conversation not found"));

        // Verify user is part of conversation
        if (!isUserInConversation(user, conversation)) {
            throw new Exception("Unauthorized");
        }

        // Check for existing pending proposal
        Optional<VideoDate> existingProposal = videoDateRepo
                .findByConversationAndStatus(conversation, DateStatus.PROPOSED);

        if (existingProposal.isPresent()) {
            return Map.of(
                    "success", false,
                    "error", "There is already a pending video date proposal",
                    "existingProposal", mapVideoDateToResponse(existingProposal.get())
            );
        }

        // Get other user
        User otherUser = getOtherUser(user, conversation);

        // Create video date proposal
        VideoDate videoDate = new VideoDate();
        videoDate.setConversation(conversation);
        videoDate.setUserA(user);
        videoDate.setUserB(otherUser);
        videoDate.setScheduledAt(proposedTime);
        videoDate.setStatus(DateStatus.PROPOSED);

        videoDateRepo.save(videoDate);

        return Map.of(
                "success", true,
                "videoDate", mapVideoDateToResponse(videoDate),
                "message", "Video date proposed! Waiting for response."
        );
    }

    public Map<String, Object> respondToProposal(Long videoDateId, boolean accept, Date counterTime) throws Exception {
        User user = authService.getCurrentUser(true);

        VideoDate videoDate = videoDateRepo.findById(videoDateId)
                .orElseThrow(() -> new Exception("Video date not found"));

        // Verify user is userB (the recipient)
        if (!videoDate.getUserB().getId().equals(user.getId())) {
            throw new Exception("Unauthorized");
        }

        if (videoDate.getStatus() != DateStatus.PROPOSED) {
            throw new Exception("This proposal is no longer active");
        }

        if (accept) {
            videoDate.setStatus(DateStatus.ACCEPTED);
            if (counterTime != null) {
                videoDate.setScheduledAt(counterTime);
            }

            // Generate room URL (in production, integrate with video provider)
            videoDate.setRoomUrl(generateRoomUrl(videoDate));

            videoDateRepo.save(videoDate);

            return Map.of(
                    "success", true,
                    "videoDate", mapVideoDateToResponse(videoDate),
                    "message", "Video date accepted!",
                    "roomUrl", videoDate.getRoomUrl()
            );
        } else {
            videoDate.setStatus(DateStatus.CANCELLED);
            videoDateRepo.save(videoDate);

            return Map.of(
                    "success", true,
                    "message", "Video date declined"
            );
        }
    }

    public Map<String, Object> startVideoDate(Long videoDateId) throws Exception {
        User user = authService.getCurrentUser(true);

        VideoDate videoDate = videoDateRepo.findById(videoDateId)
                .orElseThrow(() -> new Exception("Video date not found"));

        if (!isUserInVideoDate(user, videoDate)) {
            throw new Exception("Unauthorized");
        }

        if (videoDate.getStatus() != DateStatus.ACCEPTED && videoDate.getStatus() != DateStatus.SCHEDULED) {
            throw new Exception("Video date cannot be started");
        }

        videoDate.setStatus(DateStatus.IN_PROGRESS);
        videoDate.setStartedAt(new Date());
        videoDateRepo.save(videoDate);

        return Map.of(
                "success", true,
                "roomUrl", videoDate.getRoomUrl(),
                "maxDuration", maxDurationSeconds
        );
    }

    public Map<String, Object> endVideoDate(Long videoDateId) throws Exception {
        User user = authService.getCurrentUser(true);

        VideoDate videoDate = videoDateRepo.findById(videoDateId)
                .orElseThrow(() -> new Exception("Video date not found"));

        if (!isUserInVideoDate(user, videoDate)) {
            throw new Exception("Unauthorized");
        }

        if (videoDate.getStatus() != DateStatus.IN_PROGRESS) {
            throw new Exception("Video date is not in progress");
        }

        videoDate.setStatus(DateStatus.COMPLETED);
        videoDate.setEndedAt(new Date());

        // Calculate duration
        if (videoDate.getStartedAt() != null) {
            long durationMs = videoDate.getEndedAt().getTime() - videoDate.getStartedAt().getTime();
            videoDate.setDurationSeconds((int) (durationMs / 1000));
        }

        videoDateRepo.save(videoDate);

        // Record positive behavior for both users
        reputationService.recordBehavior(videoDate.getUserA(),
                UserBehaviorEvent.BehaviorType.COMPLETED_DATE,
                videoDate.getUserB(),
                Map.of("duration", videoDate.getDurationSeconds()));

        reputationService.recordBehavior(videoDate.getUserB(),
                UserBehaviorEvent.BehaviorType.COMPLETED_DATE,
                videoDate.getUserA(),
                Map.of("duration", videoDate.getDurationSeconds()));

        // Show donation prompt after completing a video date
        donationService.showAfterDatePrompt(user);

        return Map.of(
                "success", true,
                "duration", videoDate.getDurationSeconds(),
                "message", "Video date completed!"
        );
    }

    public Map<String, Object> submitFeedback(Long videoDateId, Map<String, Object> feedback) throws Exception {
        User user = authService.getCurrentUser(true);

        VideoDate videoDate = videoDateRepo.findById(videoDateId)
                .orElseThrow(() -> new Exception("Video date not found"));

        if (!isUserInVideoDate(user, videoDate)) {
            throw new Exception("Unauthorized");
        }

        if (videoDate.getStatus() != DateStatus.COMPLETED) {
            throw new Exception("Feedback can only be submitted for completed dates");
        }

        String feedbackJson = objectMapper.writeValueAsString(feedback);

        if (videoDate.getUserA().getId().equals(user.getId())) {
            videoDate.setUserAFeedback(feedbackJson);
        } else {
            videoDate.setUserBFeedback(feedbackJson);
        }

        videoDateRepo.save(videoDate);

        // Process feedback for reputation
        processFeedback(user, videoDate, feedback);

        return Map.of(
                "success", true,
                "message", "Thank you for your feedback!"
        );
    }

    public Map<String, Object> getUpcomingDates() throws Exception {
        User user = authService.getCurrentUser(true);

        List<VideoDate> upcoming = videoDateRepo.findByUserAndStatus(user, DateStatus.ACCEPTED);
        upcoming.addAll(videoDateRepo.findByUserAndStatus(user, DateStatus.SCHEDULED));

        upcoming.sort(Comparator.comparing(VideoDate::getScheduledAt));

        return Map.of(
                "dates", upcoming.stream()
                        .map(this::mapVideoDateToResponse)
                        .collect(Collectors.toList())
        );
    }

    public Map<String, Object> getPendingProposals() throws Exception {
        User user = authService.getCurrentUser(true);

        List<VideoDate> proposals = videoDateRepo.findByUserAndStatus(user, DateStatus.PROPOSED)
                .stream()
                .filter(vd -> vd.getUserB().getId().equals(user.getId()))
                .collect(Collectors.toList());

        return Map.of(
                "proposals", proposals.stream()
                        .map(this::mapVideoDateToResponse)
                        .collect(Collectors.toList())
        );
    }

    public Map<String, Object> getDateHistory() throws Exception {
        User user = authService.getCurrentUser(true);

        List<VideoDate> allDates = videoDateRepo.findByUserAOrUserB(user, user);

        // Filter to completed and cancelled dates
        List<VideoDate> history = allDates.stream()
                .filter(vd -> vd.getStatus() == DateStatus.COMPLETED ||
                        vd.getStatus() == DateStatus.CANCELLED ||
                        vd.getStatus() == DateStatus.NO_SHOW_A ||
                        vd.getStatus() == DateStatus.NO_SHOW_B)
                .sorted(Comparator.comparing(VideoDate::getCreatedAt).reversed())
                .limit(20)
                .collect(Collectors.toList());

        return Map.of(
                "history", history.stream()
                        .map(this::mapVideoDateToResponse)
                        .collect(Collectors.toList())
        );
    }

    public void handleNoShow(Long videoDateId, User noShowUser) {
        try {
            VideoDate videoDate = videoDateRepo.findById(videoDateId).orElse(null);
            if (videoDate == null) return;

            if (noShowUser.getId().equals(videoDate.getUserA().getId())) {
                videoDate.setStatus(DateStatus.NO_SHOW_A);
                reputationService.recordBehavior(videoDate.getUserA(),
                        UserBehaviorEvent.BehaviorType.NO_SHOW, videoDate.getUserB(), null);
            } else {
                videoDate.setStatus(DateStatus.NO_SHOW_B);
                reputationService.recordBehavior(videoDate.getUserB(),
                        UserBehaviorEvent.BehaviorType.NO_SHOW, videoDate.getUserA(), null);
            }

            videoDateRepo.save(videoDate);
        } catch (Exception e) {
            LOGGER.error("Failed to handle no-show", e);
        }
    }

    private boolean isUserInConversation(User user, Conversation conversation) {
        return conversation.getUsers().stream()
                .anyMatch(u -> u.getId().equals(user.getId()));
    }

    private boolean isUserInVideoDate(User user, VideoDate videoDate) {
        return videoDate.getUserA().getId().equals(user.getId()) ||
                videoDate.getUserB().getId().equals(user.getId());
    }

    private User getOtherUser(User user, Conversation conversation) {
        return conversation.getUsers().stream()
                .filter(u -> !u.getId().equals(user.getId()))
                .findFirst()
                .orElse(null);
    }

    private String generateRoomUrl(VideoDate videoDate) {
        String roomId;
        if (videoDate != null && videoDate.getId() != null) {
            String raw = "video-date:" + videoDate.getId() + ":" +
                    (videoDate.getScheduledAt() != null ? videoDate.getScheduledAt().getTime() : System.currentTimeMillis());
            roomId = UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
        } else {
            roomId = UUID.randomUUID().toString().replace("-", "");
        }
        String shortId = roomId.substring(0, Math.min(roomId.length(), 16));
        return roomBaseUrl.endsWith("/") ? roomBaseUrl + shortId : roomBaseUrl + "/" + shortId;
    }

    private Map<String, Object> mapVideoDateToResponse(VideoDate videoDate) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", videoDate.getId());
        response.put("status", videoDate.getStatus().name());
        response.put("scheduledAt", videoDate.getScheduledAt());
        response.put("createdAt", videoDate.getCreatedAt());

        if (videoDate.getUserA() != null) {
            response.put("proposedBy", Map.of(
                    "id", videoDate.getUserA().getId(),
                    "uuid", videoDate.getUserA().getUuid().toString(),
                    "name", videoDate.getUserA().getFirstName()
            ));
        }

        if (videoDate.getUserB() != null) {
            response.put("proposedTo", Map.of(
                    "id", videoDate.getUserB().getId(),
                    "uuid", videoDate.getUserB().getUuid().toString(),
                    "name", videoDate.getUserB().getFirstName()
            ));
        }

        if (videoDate.getStatus() == DateStatus.COMPLETED) {
            response.put("duration", videoDate.getDurationSeconds());
            response.put("startedAt", videoDate.getStartedAt());
            response.put("endedAt", videoDate.getEndedAt());
        }

        if (videoDate.getStatus() == DateStatus.IN_PROGRESS ||
                videoDate.getStatus() == DateStatus.ACCEPTED) {
            response.put("roomUrl", videoDate.getRoomUrl());
        }

        return response;
    }

    private void processFeedback(User fromUser, VideoDate videoDate, Map<String, Object> feedback) {
        User otherUser = fromUser.getId().equals(videoDate.getUserA().getId())
                ? videoDate.getUserB() : videoDate.getUserA();

        // Check overall rating
        Object ratingObj = feedback.get("rating");
        if (ratingObj instanceof Number) {
            int rating = ((Number) ratingObj).intValue();
            if (rating >= 4) {
                reputationService.recordBehavior(otherUser,
                        UserBehaviorEvent.BehaviorType.POSITIVE_FEEDBACK, fromUser,
                        Map.of("rating", rating));
            } else if (rating <= 2) {
                reputationService.recordBehavior(otherUser,
                        UserBehaviorEvent.BehaviorType.NEGATIVE_FEEDBACK, fromUser,
                        Map.of("rating", rating));
            }
        }
    }

    /**
     * Convenience method for scheduling a video date by participant UUID.
     * Creates or finds a conversation between users first.
     */
    public VideoDate scheduleDate(String participantUuid, Date scheduledTime, int durationMinutes) throws Exception {
        User initiator = authService.getCurrentUser(true);
        User participant = userRepo.findOptionalByUuid(UUID.fromString(participantUuid))
                .orElseThrow(() -> new Exception("Participant not found"));

        if (scheduledTime.before(new Date())) {
            throw new Exception("Cannot schedule video date in the past");
        }

        if (initiator.getId().equals(participant.getId())) {
            throw new Exception("Cannot schedule video date with yourself");
        }

        // Find or create conversation
        Conversation conversation = conversationRepo.findByUsers(initiator.getId(), participant.getId())
                .orElseGet(() -> {
                    Conversation newConv = new Conversation();
                    newConv.addUser(initiator);
                    newConv.addUser(participant);
                    return conversationRepo.save(newConv);
                });

        // Create video date as a proposal for the participant to accept/decline
        VideoDate videoDate = new VideoDate();
        videoDate.setConversation(conversation);
        videoDate.setUserA(initiator);
        videoDate.setUserB(participant);
        videoDate.setScheduledAt(scheduledTime);
        videoDate.setDurationSeconds(durationMinutes * 60);
        videoDate.setStatus(VideoDate.DateStatus.PROPOSED);

        return videoDateRepo.save(videoDate);
    }

    /**
     * Convenience method: Accept a video date proposal.
     */
    public VideoDate acceptDate(String videoDateId) throws Exception {
        Long id = Long.parseLong(videoDateId);
        VideoDate videoDate = videoDateRepo.findById(id)
                .orElseThrow(() -> new Exception("Video date not found"));
        respondToProposal(id, true, videoDate.getScheduledAt());
        return videoDateRepo.findById(id).orElseThrow();
    }

    /**
     * Convenience method: Decline a video date proposal.
     */
    public VideoDate declineDate(String videoDateId, String reason) throws Exception {
        Long id = Long.parseLong(videoDateId);
        respondToProposal(id, false, null);
        VideoDate videoDate = videoDateRepo.findById(id).orElseThrow();
        videoDate.setStatus(DateStatus.CANCELLED);
        return videoDateRepo.save(videoDate);
    }

    /**
     * Convenience method: Cancel a video date.
     */
    public VideoDate cancelDate(String videoDateId, String reason) throws Exception {
        Long id = Long.parseLong(videoDateId);
        VideoDate videoDate = videoDateRepo.findById(id)
                .orElseThrow(() -> new Exception("Video date not found"));
        videoDate.setStatus(DateStatus.CANCELLED);
        return videoDateRepo.save(videoDate);
    }

    /**
     * Convenience method: Reschedule a video date.
     * Rescheduling sets the status back to PROPOSED so participant can accept new time.
     */
    public VideoDate rescheduleDate(String videoDateId, Date newTime) throws Exception {
        Long id = Long.parseLong(videoDateId);
        VideoDate videoDate = videoDateRepo.findById(id)
                .orElseThrow(() -> new Exception("Video date not found"));
        videoDate.setScheduledAt(newTime);
        videoDate.setStatus(DateStatus.PROPOSED);
        return videoDateRepo.save(videoDate);
    }

    /**
     * Convenience method: Complete a video date.
     */
    public VideoDate completeDate(String videoDateId) throws Exception {
        endVideoDate(Long.parseLong(videoDateId));
        return videoDateRepo.findById(Long.parseLong(videoDateId)).orElseThrow();
    }
}
