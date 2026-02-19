package com.nonononoki.alovoa.service;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserNotification;
import com.nonononoki.alovoa.repo.UserNotificationRepository;
import com.nonononoki.alovoa.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Service for handling in-app and email notifications.
 */
@Service
public class NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);

    private static final String TYPE_SYSTEM = "SYSTEM";

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private UserNotificationRepository notificationRepo;

    @Autowired
    private MailService mailService;

    /**
     * Compatibility wrapper for push notification calls.
     */
    public void sendPushNotification(Long userId, String title, String body) {
        if (userId == null) {
            return;
        }

        User recipient = userRepo.findById(userId).orElse(null);
        if (recipient == null) {
            LOGGER.warn("Cannot send notification to missing user {}", userId);
            return;
        }
        LOGGER.debug("Push provider not configured, skipping delivery to {}: {}",
                recipient.getId(), mergeMessage(title, body));
    }

    /**
     * Send an email notification.
     */
    public void sendEmailNotification(String email, String subject, String body) {
        if (email == null || email.isBlank()) {
            return;
        }
        mailService.sendAdminMail(email, safeText(subject), safeText(body));
    }

    /**
     * Create an in-app notification row.
     */
    public void sendUserNotification(User recipient, User sender, String notificationType, String message) {
        if (recipient == null) {
            return;
        }

        UserNotification notification = new UserNotification();
        notification.setUserTo(recipient);
        notification.setUserFrom(sender);
        notification.setContent(notificationType != null ? notificationType : TYPE_SYSTEM);
        notification.setNotificationType(notificationType != null ? notificationType : TYPE_SYSTEM);
        notification.setMessage(safeText(message));
        notification.setDate(new Date());
        notification.setReadStatus(false);
        notificationRepo.save(notification);
    }

    private String mergeMessage(String title, String body) {
        String cleanTitle = safeText(title).trim();
        String cleanBody = safeText(body).trim();
        if (cleanTitle.isEmpty()) {
            return cleanBody;
        }
        if (cleanBody.isEmpty()) {
            return cleanTitle;
        }
        return cleanTitle + ": " + cleanBody;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }
}
