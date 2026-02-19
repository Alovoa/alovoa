package com.nonononoki.alovoa.entity.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nonononoki.alovoa.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

/**
 * User's calendar integration settings.
 * Supports Google Calendar, Apple Calendar (CalDAV), and Outlook.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
public class UserCalendarSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // === Google Calendar ===
    private boolean googleCalendarEnabled = false;

    @JsonIgnore  // Don't expose tokens
    @Column(length = 500)
    private String googleRefreshToken;

    private String googleCalendarId;

    // === Apple Calendar (CalDAV) ===
    private boolean appleCalendarEnabled = false;

    @JsonIgnore
    @Column(length = 500)
    private String appleCaldavUrl;

    private String appleCalendarId;

    // === Outlook/Microsoft Calendar ===
    private boolean outlookCalendarEnabled = false;

    @JsonIgnore
    @Column(length = 500)
    private String outlookRefreshToken;

    private String outlookCalendarId;

    // === General Settings ===

    /**
     * Default reminder time before dates (in minutes)
     */
    private int defaultReminderMinutes = 60;

    /**
     * Automatically add confirmed dates to calendar
     */
    private boolean autoAddDates = true;

    /**
     * Show actual match name vs generic "AURA Date" for privacy
     */
    private boolean showMatchName = false;

    /**
     * JSON blob containing weekly availability settings used by /api/v1/calendar.
     */
    @Lob
    @Column(columnDefinition = "text")
    private String weeklyAvailabilityJson;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        updatedAt = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }

    // === Helper Methods ===

    public boolean hasAnyCalendarEnabled() {
        return googleCalendarEnabled || appleCalendarEnabled || outlookCalendarEnabled;
    }

    public CalendarProvider getPrimaryProvider() {
        if (googleCalendarEnabled) return CalendarProvider.GOOGLE;
        if (appleCalendarEnabled) return CalendarProvider.APPLE;
        if (outlookCalendarEnabled) return CalendarProvider.OUTLOOK;
        return null;
    }

    public enum CalendarProvider {
        GOOGLE,
        APPLE,
        OUTLOOK
    }
}
