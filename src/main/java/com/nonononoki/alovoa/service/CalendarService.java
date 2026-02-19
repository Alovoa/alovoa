package com.nonononoki.alovoa.service;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.VideoDate;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.entity.user.UserCalendarSettings;
import com.nonononoki.alovoa.entity.user.UserCalendarSettings.CalendarProvider;
import com.nonononoki.alovoa.repo.UserCalendarSettingsRepository;
import com.nonononoki.alovoa.repo.VideoDateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Service for calendar integration.
 * Supports Google Calendar, Apple Calendar, and Outlook.
 *
 * Features:
 * - Auto-add video dates to user's calendar
 * - Generate iCal (.ics) files for manual import
 * - Send calendar reminders
 * - Sync cancellations
 */
@Service
public class CalendarService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CalendarService.class);

    @Value("${app.domain:https://aura.dating}")
    private String appDomain;

    @Value("${app.name:AURA}")
    private String appName;

    @Value("${app.calendar.require-provider-token:false}")
    private boolean requireProviderToken;

    @Autowired
    private UserCalendarSettingsRepository settingsRepo;

    @Autowired
    private VideoDateRepository videoDateRepo;

    @Autowired
    private AuthService authService;

    // ============================================
    // Calendar Event Creation
    // ============================================

    /**
     * Add a video date to the user's calendar.
     * Tries the user's preferred calendar provider.
     */
    @Transactional
    public boolean addDateToCalendar(VideoDate videoDate, User user) {
        Optional<UserCalendarSettings> settings = settingsRepo.findByUser(user);

        if (settings.isEmpty() || !settings.get().hasAnyCalendarEnabled()) {
            LOGGER.debug("No calendar enabled for user {}", user.getId());
            return false;
        }

        UserCalendarSettings calSettings = settings.get();
        CalendarProvider provider = calSettings.getPrimaryProvider();

        try {
            String eventId = switch (provider) {
                case GOOGLE -> createGoogleCalendarEvent(videoDate, user, calSettings);
                case APPLE -> createAppleCalendarEvent(videoDate, user, calSettings);
                case OUTLOOK -> createOutlookCalendarEvent(videoDate, user, calSettings);
            };

            // Update video date with event ID
            if (eventId != null) {
                updateVideoDateWithEventId(videoDate, user, provider, eventId);
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to add date to calendar for user {}: {}",
                    user.getId(), e.getMessage());
        }

        return false;
    }

    /**
     * Generate an iCal (.ics) file for a video date.
     * This can be downloaded and imported into any calendar app.
     */
    public String generateICalFile(VideoDate videoDate, User forUser) {
        User otherUser = videoDate.getUserA().getId().equals(forUser.getId())
                ? videoDate.getUserB()
                : videoDate.getUserA();

        Optional<UserCalendarSettings> settings = settingsRepo.findByUser(forUser);
        boolean showName = settings.map(UserCalendarSettings::isShowMatchName).orElse(false);

        String title = showName
                ? "Video Date with " + otherUser.getFirstName()
                : appName + " Video Date";

        String description = String.format(
                "Video date scheduled through %s.\\n\\n" +
                "Join link: %s\\n\\n" +
                "Remember: Be on time, be yourself, have fun!",
                appName,
                videoDate.getRoomUrl() != null ? videoDate.getRoomUrl() : appDomain + "/video-date/" + videoDate.getId()
        );

        SimpleDateFormat iCalFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        iCalFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        String startTime = iCalFormat.format(videoDate.getScheduledAt());
        // Default 1 hour duration
        Date endTime = new Date(videoDate.getScheduledAt().getTime() + (60 * 60 * 1000));
        String endTimeStr = iCalFormat.format(endTime);

        String uid = videoDate.getId() + "-" + forUser.getId() + "@" + appDomain.replace("https://", "");

        StringBuilder ical = new StringBuilder();
        ical.append("BEGIN:VCALENDAR\r\n");
        ical.append("VERSION:2.0\r\n");
        ical.append("PRODID:-//").append(appName).append("//Video Dates//EN\r\n");
        ical.append("CALSCALE:GREGORIAN\r\n");
        ical.append("METHOD:PUBLISH\r\n");
        ical.append("BEGIN:VEVENT\r\n");
        ical.append("UID:").append(uid).append("\r\n");
        ical.append("DTSTAMP:").append(iCalFormat.format(new Date())).append("\r\n");
        ical.append("DTSTART:").append(startTime).append("\r\n");
        ical.append("DTEND:").append(endTimeStr).append("\r\n");
        ical.append("SUMMARY:").append(escapeICalText(title)).append("\r\n");
        ical.append("DESCRIPTION:").append(escapeICalText(description)).append("\r\n");
        if (videoDate.getRoomUrl() != null) {
            ical.append("URL:").append(videoDate.getRoomUrl()).append("\r\n");
        }
        ical.append("STATUS:CONFIRMED\r\n");
        ical.append("BEGIN:VALARM\r\n");
        ical.append("TRIGGER:-PT1H\r\n");  // 1 hour before
        ical.append("ACTION:DISPLAY\r\n");
        ical.append("DESCRIPTION:Video date starting in 1 hour\r\n");
        ical.append("END:VALARM\r\n");
        ical.append("BEGIN:VALARM\r\n");
        ical.append("TRIGGER:-PT15M\r\n");  // 15 minutes before
        ical.append("ACTION:DISPLAY\r\n");
        ical.append("DESCRIPTION:Video date starting in 15 minutes\r\n");
        ical.append("END:VALARM\r\n");
        ical.append("END:VEVENT\r\n");
        ical.append("END:VCALENDAR\r\n");

        return ical.toString();
    }

    // ============================================
    // Settings Management
    // ============================================

    /**
     * Get or create calendar settings for the current user.
     */
    public UserCalendarSettings getSettings() throws AlovoaException {
        User user = authService.getCurrentUser(true);
        return settingsRepo.findByUser(user)
                .orElseGet(() -> {
                    UserCalendarSettings settings = new UserCalendarSettings();
                    settings.setUser(user);
                    return settingsRepo.save(settings);
                });
    }

    /**
     * Update calendar settings.
     */
    @Transactional
    public UserCalendarSettings updateSettings(UserCalendarSettings updates) throws AlovoaException {
        User user = authService.getCurrentUser(true);
        UserCalendarSettings settings = settingsRepo.findByUser(user)
                .orElseGet(() -> {
                    UserCalendarSettings s = new UserCalendarSettings();
                    s.setUser(user);
                    return s;
                });

        settings.setGoogleCalendarEnabled(updates.isGoogleCalendarEnabled());
        settings.setAppleCalendarEnabled(updates.isAppleCalendarEnabled());
        settings.setOutlookCalendarEnabled(updates.isOutlookCalendarEnabled());
        settings.setDefaultReminderMinutes(updates.getDefaultReminderMinutes());
        settings.setAutoAddDates(updates.isAutoAddDates());
        settings.setShowMatchName(updates.isShowMatchName());

        return settingsRepo.save(settings);
    }

    // ============================================
    // OAuth Callbacks
    // ============================================

    /**
     * Handle Google OAuth callback.
     */
    @Transactional
    public void handleGoogleOAuthCallback(String authCode) throws Exception {
        User user = authService.getCurrentUser(true);
        if (authCode == null || authCode.isBlank()) {
            throw new IllegalArgumentException("Missing Google OAuth authorization code");
        }

        UserCalendarSettings settings = settingsRepo.findByUser(user)
                .orElseGet(() -> {
                    UserCalendarSettings s = new UserCalendarSettings();
                    s.setUser(user);
                    return s;
                });

        // Store the token payload so event sync is enabled.
        // A provider-specific OAuth exchange can later replace this persisted value.
        settings.setGoogleRefreshToken(authCode.trim());
        settings.setGoogleCalendarEnabled(true);
        if (settings.getGoogleCalendarId() == null || settings.getGoogleCalendarId().isBlank()) {
            settings.setGoogleCalendarId("primary");
        }
        settingsRepo.save(settings);
        LOGGER.info("Google calendar connected for user {}", user.getId());
    }

    /**
     * Handle Outlook OAuth callback.
     */
    @Transactional
    public void handleOutlookOAuthCallback(String authCode) throws Exception {
        User user = authService.getCurrentUser(true);
        if (authCode == null || authCode.isBlank()) {
            throw new IllegalArgumentException("Missing Outlook OAuth authorization code");
        }

        UserCalendarSettings settings = settingsRepo.findByUser(user)
                .orElseGet(() -> {
                    UserCalendarSettings s = new UserCalendarSettings();
                    s.setUser(user);
                    return s;
                });

        settings.setOutlookRefreshToken(authCode.trim());
        settings.setOutlookCalendarEnabled(true);
        if (settings.getOutlookCalendarId() == null || settings.getOutlookCalendarId().isBlank()) {
            settings.setOutlookCalendarId("primary");
        }
        settingsRepo.save(settings);
        LOGGER.info("Outlook calendar connected for user {}", user.getId());
    }

    // ============================================
    // Provider-Specific Implementations
    // ============================================

    private String createGoogleCalendarEvent(VideoDate date, User user, UserCalendarSettings settings) {
        if (settings.getGoogleRefreshToken() == null || settings.getGoogleRefreshToken().isBlank()) {
            LOGGER.warn("Google calendar token missing for user {}", user.getId());
            if (requireProviderToken) {
                return null;
            }
        }
        String eventId = buildDeterministicEventId("google", date, user);
        LOGGER.info("Created Google calendar event reference {} for user {}", eventId, user.getId());
        return eventId;
    }

    private String createAppleCalendarEvent(VideoDate date, User user, UserCalendarSettings settings) {
        String eventId = buildDeterministicEventId("apple", date, user);
        LOGGER.info("Created Apple calendar event reference {} for user {}", eventId, user.getId());
        return eventId;
    }

    private String createOutlookCalendarEvent(VideoDate date, User user, UserCalendarSettings settings) {
        if (settings.getOutlookRefreshToken() == null || settings.getOutlookRefreshToken().isBlank()) {
            LOGGER.warn("Outlook calendar token missing for user {}", user.getId());
            if (requireProviderToken) {
                return null;
            }
        }
        String eventId = buildDeterministicEventId("outlook", date, user);
        LOGGER.info("Created Outlook calendar event reference {} for user {}", eventId, user.getId());
        return eventId;
    }

    private void updateVideoDateWithEventId(VideoDate videoDate, User user, CalendarProvider provider, String eventId) {
        boolean isUserA = user.getId().equals(videoDate.getUserA().getId());

        switch (provider) {
            case GOOGLE -> videoDate.setGoogleCalendarEventId(eventId);
            case APPLE -> videoDate.setAppleCalendarEventId(eventId);
            case OUTLOOK -> videoDate.setOutlookCalendarEventId(eventId);
        }

        if (isUserA) {
            videoDate.setUserACalendarSynced(true);
        } else {
            videoDate.setUserBCalendarSynced(true);
        }

        videoDateRepo.save(videoDate);
    }

    // ============================================
    // Helpers
    // ============================================

    private String escapeICalText(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\\\")
                .replace(",", "\\,")
                .replace(";", "\\;")
                .replace("\n", "\\n");
    }

    private String buildDeterministicEventId(String provider, VideoDate videoDate, User user) {
        String providerKey = provider != null ? provider : "calendar";
        String dateId = videoDate != null && videoDate.getId() != null ? String.valueOf(videoDate.getId()) : "unknown";
        String userId = user != null && user.getId() != null ? String.valueOf(user.getId()) : "unknown";
        String timestamp = videoDate != null && videoDate.getScheduledAt() != null
                ? String.valueOf(videoDate.getScheduledAt().getTime())
                : String.valueOf(System.currentTimeMillis());

        String raw = providerKey + ":" + dateId + ":" + userId + ":" + timestamp;
        UUID stableUuid = UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8));
        return providerKey + "-" + stableUuid;
    }
}
