package com.nonononoki.alovoa.repo;

import com.nonononoki.alovoa.entity.user.UserImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserImageRepository extends JpaRepository<UserImage, Long> {
    //@Cacheable("UserImageRepository.findByUuid")
    UserImage findByUuid(UUID uuid);
}

