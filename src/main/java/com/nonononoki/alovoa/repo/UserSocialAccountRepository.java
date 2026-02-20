package com.nonononoki.alovoa.repo;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserSocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccount, Long> {
    List<UserSocialAccount> findByUserOrderByLinkedAtDesc(User user);

    Optional<UserSocialAccount> findByUserAndProvider(User user, String provider);

    Optional<UserSocialAccount> findByProviderAndProviderUserId(String provider, String providerUserId);

    long deleteByUserAndProvider(User user, String provider);
}
