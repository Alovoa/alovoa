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
@Table(name = "user_social_account", uniqueConstraints = {
        @UniqueConstraint(name = "uk_social_account_uuid", columnNames = "uuid"),
        @UniqueConstraint(name = "uk_social_account_provider_user", columnNames = {"provider", "provider_user_id"}),
        @UniqueConstraint(name = "uk_social_account_user_provider", columnNames = {"user_id", "provider"})
}, indexes = {
        @Index(name = "idx_social_account_user", columnList = "user_id"),
        @Index(name = "idx_social_account_provider", columnList = "provider")
})
public class UserSocialAccount {

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

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Column(name = "provider_username", length = 255)
    private String providerUsername;

    @Column(name = "profile_url", length = 1024)
    private String profileUrl;

    @Column(name = "linked_at", nullable = false)
    private Date linkedAt;

    @Column(name = "updated_at", nullable = false)
    private Date updatedAt;

    @PrePersist
    protected void onCreate() {
        Date now = new Date();
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        if (linkedAt == null) {
            linkedAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }
}
