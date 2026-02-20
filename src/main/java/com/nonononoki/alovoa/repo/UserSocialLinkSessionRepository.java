package com.nonononoki.alovoa.repo;

import com.nonononoki.alovoa.entity.user.UserSocialLinkSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserSocialLinkSessionRepository extends JpaRepository<UserSocialLinkSession, Long> {
    Optional<UserSocialLinkSession> findByProviderAndStateToken(String provider, String stateToken);

    Optional<UserSocialLinkSession> findByUuid(UUID uuid);
}
