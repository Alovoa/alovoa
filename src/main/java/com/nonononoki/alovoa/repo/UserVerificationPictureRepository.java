package com.nonononoki.alovoa.repo;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserProfilePicture;
import com.nonononoki.alovoa.entity.user.UserVerificationPicture;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserVerificationPictureRepository extends JpaRepository<UserVerificationPicture, Long> {
    List<UserVerificationPicture> findTop20ByVerifiedByAdminFalseOrderByDateAsc();

    List<UserVerificationPicture> findByUserNo(User user);

    List<UserVerificationPicture> findByUserYes(User user);

    @Cacheable("UserVerificationPictureRepository.findByUuid")
    UserVerificationPicture findByUuid(UUID uuid);
}

