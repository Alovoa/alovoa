package com.nonononoki.alovoa.entity.user;

import com.nonononoki.alovoa.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "user_social_link_session", uniqueConstraints = {
        @UniqueConstraint(name = "uk_social_link_session_uuid", columnNames = "uuid"),
        @UniqueConstraint(name = "uk_social_link_session_state", columnNames = "state_token")
}, indexes = {
        @Index(name = "idx_social_link_session_user_status", columnList = "user_id,status"),
        @Index(name = "idx_social_link_session_provider_state", columnList = "provider,state_token"),
        @Index(name = "idx_social_link_session_expires_at", columnList = "expires_at")
})
public class UserSocialLinkSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, unique = true, length = 36)
    private UUID uuid;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 32)
    private String provider;

    @Column(name = "state_token", nullable = false, length = 255)
    private String stateToken;

    @Column(name = "code_verifier", length = 255)
    private String codeVerifier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LinkStatus status;

    @Column(name = "error_message", length = 255)
    private String errorMessage;

    @Column(name = "provider_user_id", length = 255)
    private String providerUserId;

    @Column(name = "provider_username", length = 255)
    private String providerUsername;

    @Column(name = "started_at", nullable = false)
    private Date startedAt;

    @Column(name = "expires_at", nullable = false)
    private Date expiresAt;

    @Column(name = "completed_at")
    private Date completedAt;

    @Column(name = "updated_at", nullable = false)
    private Date updatedAt;

    @PrePersist
    protected void onCreate() {
        Date now = new Date();
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        if (status == null) {
            status = LinkStatus.PENDING;
        }
        if (startedAt == null) {
            startedAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }

    public enum LinkStatus {
        PENDING,
        LINKED,
        FAILED,
        EXPIRED
    }
}
