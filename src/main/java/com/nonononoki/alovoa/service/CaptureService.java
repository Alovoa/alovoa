package com.nonononoki.alovoa.service;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nonononoki.alovoa.entity.CaptureSession;
import com.nonononoki.alovoa.entity.CaptureSession.CaptureStatus;
import com.nonononoki.alovoa.entity.CaptureSession.CaptureType;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.repo.CaptureSessionRepository;

import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

/**
 * Service for web-based video/screen capture (Tier A - single presigned PUT upload).
 * Handles session creation, upload URL generation, and upload verification.
 */
@Service
public class CaptureService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaptureService.class);

    private static final String CAPTURE_PREFIX = "capture/";
    private static final String WEBM_VIDEO_MIME_TYPE = "video/webm";
    private static final String WEBM_AUDIO_MIME_TYPE = "audio/webm";
    private static final int DEFAULT_VIDEO_BITRATE_BPS = 2_500_000;
    private static final int DEFAULT_AUDIO_BITRATE_BPS = 64_000;

    // Max file size: 100MB for 2-3 min 720p @ 2.5Mbps
    private static final long MAX_VIDEO_FILE_SIZE_BYTES = 100 * 1024 * 1024;
    // Max audio file size: 10MB (audio is ~1MB per 2 min at 64kbps)
    private static final long MAX_AUDIO_FILE_SIZE_BYTES = 10 * 1024 * 1024;
    // Max pending sessions per user
    private static final int MAX_PENDING_SESSIONS = 3;

    @Autowired
    private CaptureSessionRepository captureSessionRepository;

    @Autowired
    private S3StorageService s3StorageService;

    @Autowired
    private AuthService authService;

    @Value("${app.capture.presigned-url-expiry-minutes:60}")
    private int presignedUrlExpiryMinutes;

    @Value("${app.capture.enabled:true}")
    private boolean captureEnabled;

    /**
     * Create a new capture session and return session info with presigned upload URL.
     *
     * @param captureType Type of capture (screen, webcam, etc.)
     * @return Map containing captureId, s3Key, putUrl, expiresIn
     */
    @Transactional
    public Map<String, Object> createSession(CaptureType captureType) throws Exception {
        if (!captureEnabled) {
            throw new IllegalStateException("Capture feature is disabled");
        }

        if (!s3StorageService.isEnabled()) {
            throw new IllegalStateException("S3 storage is not available");
        }

        User user = authService.getCurrentUser(true);

        // Check for too many pending sessions
        long pendingCount = captureSessionRepository.countPendingByUser(user);
        if (pendingCount >= MAX_PENDING_SESSIONS) {
            throw new IllegalStateException("Too many pending capture sessions. Please complete or cancel existing sessions.");
        }

        // Create session
        CaptureSession session = new CaptureSession();
        session.setUser(user);
        CaptureType type = captureType != null ? captureType : CaptureType.SCREEN_MIC;
        session.setCaptureType(type);
        session.setStatus(CaptureStatus.PENDING);

        // Determine MIME type and file extension based on capture type
        boolean isAudioOnly = type == CaptureType.AUDIO_ONLY;
        String mimeType = isAudioOnly ? WEBM_AUDIO_MIME_TYPE : WEBM_VIDEO_MIME_TYPE;
        String extension = isAudioOnly ? ".webm" : ".webm";  // Both use webm container
        long maxSize = isAudioOnly ? MAX_AUDIO_FILE_SIZE_BYTES : MAX_VIDEO_FILE_SIZE_BYTES;

        session.setMimeType(mimeType);

        // Generate S3 key
        String s3Key = CAPTURE_PREFIX + user.getId() + "/" + session.getCaptureId() + extension;
        session.setS3Key(s3Key);

        captureSessionRepository.save(session);

        // Generate presigned PUT URL
        Duration expiry = Duration.ofMinutes(presignedUrlExpiryMinutes);
        String putUrl = s3StorageService.getPresignedUploadUrl(s3Key, mimeType, expiry);

        LOGGER.info("Created {} capture session {} for user {}",
            isAudioOnly ? "audio" : "video", session.getCaptureId(), user.getId());

        return Map.of(
            "captureId", session.getCaptureId().toString(),
            "s3Key", s3Key,
            "putUrl", putUrl,
            "expiresIn", presignedUrlExpiryMinutes * 60,
            "maxSizeBytes", maxSize,
            "mimeType", mimeType,
            "isAudioOnly", isAudioOnly
        );
    }

    /**
     * Get a fresh presigned upload URL for an existing session.
     * Useful if the original URL expired.
     *
     * @param captureId UUID of the capture session
     * @return Map containing putUrl and expiresIn
     */
    @Transactional(readOnly = true)
    public Map<String, Object> refreshUploadUrl(UUID captureId) throws Exception {
        User user = authService.getCurrentUser(true);

        CaptureSession session = captureSessionRepository.findByCaptureIdAndUser(captureId, user)
            .orElseThrow(() -> new IllegalArgumentException("Capture session not found"));

        if (session.getStatus() != CaptureStatus.PENDING) {
            throw new IllegalStateException("Cannot refresh URL for session in " + session.getStatus() + " status");
        }

        Duration expiry = Duration.ofMinutes(presignedUrlExpiryMinutes);
        String mimeType = session.getMimeType() != null ? session.getMimeType() : WEBM_VIDEO_MIME_TYPE;
        String putUrl = s3StorageService.getPresignedUploadUrl(session.getS3Key(), mimeType, expiry);

        return Map.of(
            "putUrl", putUrl,
            "expiresIn", presignedUrlExpiryMinutes * 60
        );
    }

    /**
     * Mark upload as complete and verify the file.
     * Called by the client after successful S3 PUT.
     *
     * @param captureId UUID of the capture session
     * @return Map containing status and metadata
     */
    @Transactional
    public Map<String, Object> confirmUpload(UUID captureId) throws Exception {
        User user = authService.getCurrentUser(true);

        CaptureSession session = captureSessionRepository.findByCaptureIdAndUser(captureId, user)
            .orElseThrow(() -> new IllegalArgumentException("Capture session not found"));

        if (session.getStatus() != CaptureStatus.PENDING && session.getStatus() != CaptureStatus.UPLOADING) {
            throw new IllegalStateException("Session not in uploadable state");
        }

        // Verify file exists in S3
        if (!s3StorageService.objectExists(session.getS3Key())) {
            throw new IllegalStateException("Upload not found in S3. Please try uploading again.");
        }

        // Get metadata
        HeadObjectResponse metadata = s3StorageService.getObjectMetadata(session.getS3Key());
        if (metadata == null) {
            throw new IllegalStateException("Could not retrieve upload metadata");
        }

        long fileSize = metadata.contentLength();
        String contentType = metadata.contentType();

        // Determine expected types based on capture type
        boolean isAudioOnly = session.getCaptureType() == CaptureType.AUDIO_ONLY;
        long maxSize = isAudioOnly ? MAX_AUDIO_FILE_SIZE_BYTES : MAX_VIDEO_FILE_SIZE_BYTES;
        String expectedPrefix = isAudioOnly ? "audio/" : "video/";

        // Validate size
        if (fileSize > maxSize) {
            // Delete oversized file
            s3StorageService.deleteMedia(session.getS3Key());
            session.setStatus(CaptureStatus.FAILED);
            session.setErrorMessage("File too large: " + (fileSize / 1024 / 1024) + "MB exceeds limit");
            captureSessionRepository.save(session);
            throw new IllegalArgumentException("File too large. Maximum size is " + (maxSize / 1024 / 1024) + "MB");
        }

        // Validate content type
        if (contentType == null || !contentType.startsWith(expectedPrefix)) {
            s3StorageService.deleteMedia(session.getS3Key());
            session.setStatus(CaptureStatus.FAILED);
            session.setErrorMessage("Invalid content type: " + contentType);
            captureSessionRepository.save(session);
            throw new IllegalArgumentException("Invalid file type. Expected " +
                (isAudioOnly ? "audio" : "video") + ", got: " + contentType);
        }

        // Update session
        session.setStatus(CaptureStatus.UPLOADED);
        session.setUploadedAt(new Date());
        session.setFileSizeBytes(fileSize);
        session.setMimeType(contentType);
        captureSessionRepository.save(session);

        LOGGER.info("Confirmed upload for session {}: {} bytes", captureId, fileSize);

        // Trigger processing pipeline for this upload.
        processUploadedSession(session.getCaptureId());

        return Map.of(
                "status", session.getStatus().name(),
                "fileSizeBytes", fileSize,
                "mimeType", contentType,
            "s3Key", session.getS3Key()
        );
    }

    /**
     * Get session status and metadata.
     *
     * @param captureId UUID of the capture session
     * @return Session info
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSessionStatus(UUID captureId) throws Exception {
        User user = authService.getCurrentUser(true);

        CaptureSession session = captureSessionRepository.findByCaptureIdAndUser(captureId, user)
            .orElseThrow(() -> new IllegalArgumentException("Capture session not found"));

        return sessionToMap(session);
    }

    /**
     * List user's capture sessions.
     *
     * @return List of session info
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listSessions() throws Exception {
        User user = authService.getCurrentUser(true);
        List<CaptureSession> sessions = captureSessionRepository.findByUserOrderByCreatedAtDesc(user);

        return sessions.stream()
            .map(this::sessionToMap)
            .collect(Collectors.toList());
    }

    /**
     * Delete a capture session and its S3 object.
     *
     * @param captureId UUID of the capture session
     */
    @Transactional
    public void deleteSession(UUID captureId) throws Exception {
        User user = authService.getCurrentUser(true);

        CaptureSession session = captureSessionRepository.findByCaptureIdAndUser(captureId, user)
            .orElseThrow(() -> new IllegalArgumentException("Capture session not found"));

        // Delete S3 object if exists
        if (session.getS3Key() != null) {
            s3StorageService.deleteMedia(session.getS3Key());
        }

        captureSessionRepository.delete(session);
        LOGGER.info("Deleted capture session {}", captureId);
    }

    /**
     * Get presigned URL for playback.
     *
     * @param captureId UUID of the capture session
     * @return Map containing playUrl and expiresIn
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPlaybackUrl(UUID captureId) throws Exception {
        User user = authService.getCurrentUser(true);

        CaptureSession session = captureSessionRepository.findByCaptureIdAndUser(captureId, user)
            .orElseThrow(() -> new IllegalArgumentException("Capture session not found"));

        if (!session.isUploaded()) {
            throw new IllegalStateException("Capture not yet uploaded");
        }

        // Prefer transcoded/HLS if available
        String urlKey = session.getTranscodedUrl() != null ? session.getTranscodedUrl() : session.getS3Key();

        Duration expiry = Duration.ofHours(1);
        String playUrl = s3StorageService.getPresignedUrl(urlKey, expiry);

        String mimeType = session.getMimeType() != null ? session.getMimeType() :
            (session.getCaptureType() == CaptureType.AUDIO_ONLY ? WEBM_AUDIO_MIME_TYPE : WEBM_VIDEO_MIME_TYPE);

        return Map.of(
            "playUrl", playUrl,
            "expiresIn", 3600,
            "mimeType", mimeType
        );
    }

    /**
     * Periodic processor for uploaded captures.
     * Handles uploads that were confirmed but not processed yet.
     */
    @Scheduled(fixedDelayString = "${app.capture.processing.poll-ms:60000}")
    @Transactional
    public void processPendingUploads() {
        List<CaptureSession> pending = captureSessionRepository.findPendingProcessing();
        if (pending.isEmpty()) {
            return;
        }

        for (CaptureSession session : pending) {
            try {
                processUploadedSession(session.getCaptureId());
            } catch (Exception e) {
                LOGGER.warn("Capture processing failed for {}: {}", session.getCaptureId(), e.getMessage());
            }
        }
    }

    /**
     * Process a capture that finished upload.
     */
    @Transactional
    public void processUploadedSession(UUID captureId) {
        if (captureId == null) {
            return;
        }

        CaptureSession session = captureSessionRepository.findByCaptureId(captureId).orElse(null);
        if (session == null) {
            return;
        }
        if (session.getStatus() != CaptureStatus.UPLOADED && session.getStatus() != CaptureStatus.PROCESSING) {
            return;
        }

        session.setStatus(CaptureStatus.PROCESSING);
        captureSessionRepository.save(session);

        try {
            if (!s3StorageService.objectExists(session.getS3Key())) {
                throw new IllegalStateException("Uploaded object no longer exists");
            }

            boolean audioOnly = session.getCaptureType() == CaptureType.AUDIO_ONLY;
            long fileSize = session.getFileSizeBytes() != null ? session.getFileSizeBytes() : 0L;
            int bitrate = audioOnly ? DEFAULT_AUDIO_BITRATE_BPS : DEFAULT_VIDEO_BITRATE_BPS;
            int estimatedDuration = estimateDurationSeconds(fileSize, bitrate);

            session.setBitrateBps(bitrate);
            if (session.getDurationSeconds() == null || session.getDurationSeconds() <= 0) {
                session.setDurationSeconds(estimatedDuration);
            }

            // Tier-A processing keeps original object as playback asset.
            session.setTranscodedUrl(session.getS3Key());
            if (!audioOnly) {
                // HLS manifest placeholder path for downstream transcoder integration.
                session.setHlsPlaylistUrl(session.getS3Key().replace(".webm", ".m3u8"));
            } else {
                session.setHlsPlaylistUrl(null);
            }
            session.setProcessedAt(new Date());
            session.setStatus(CaptureStatus.READY);
            session.setErrorMessage(null);
            captureSessionRepository.save(session);
            LOGGER.info("Capture session {} is READY", session.getCaptureId());
        } catch (Exception e) {
            session.setStatus(CaptureStatus.FAILED);
            session.setProcessedAt(new Date());
            session.setErrorMessage(e.getMessage());
            captureSessionRepository.save(session);
            LOGGER.error("Capture session {} failed processing: {}", session.getCaptureId(), e.getMessage());
        }
    }

    private Map<String, Object> sessionToMap(CaptureSession session) {
        return Map.ofEntries(
            Map.entry("captureId", session.getCaptureId().toString()),
            Map.entry("status", session.getStatus().name()),
            Map.entry("captureType", session.getCaptureType().name()),
            Map.entry("title", session.getTitle() != null ? session.getTitle() : ""),
            Map.entry("description", session.getDescription() != null ? session.getDescription() : ""),
            Map.entry("mimeType", session.getMimeType() != null ? session.getMimeType() : ""),
            Map.entry("fileSizeBytes", session.getFileSizeBytes() != null ? session.getFileSizeBytes() : 0),
            Map.entry("durationSeconds", session.getDurationSeconds() != null ? session.getDurationSeconds() : 0),
            Map.entry("createdAt", session.getCreatedAt() != null ? session.getCreatedAt().getTime() : 0),
            Map.entry("uploadedAt", session.getUploadedAt() != null ? session.getUploadedAt().getTime() : 0),
            Map.entry("isReady", session.isReady()),
            Map.entry("isFailed", session.isFailed()),
            Map.entry("errorMessage", session.getErrorMessage() != null ? session.getErrorMessage() : "")
        );
    }

    private int estimateDurationSeconds(long fileSizeBytes, int bitrateBps) {
        if (fileSizeBytes <= 0 || bitrateBps <= 0) {
            return 0;
        }
        long bits = fileSizeBytes * 8L;
        return Math.max(1, (int) (bits / bitrateBps));
    }
}
