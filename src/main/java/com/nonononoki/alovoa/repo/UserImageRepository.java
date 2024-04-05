package com.nonononoki.alovoa.repo;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.user.UserImage;

import java.util.UUID;

public interface UserImageRepository extends JpaRepository<UserImage, Long> {
    @Cacheable("UserImageRepository.findByUuid")
    UserImage findByUuid(UUID uuid);
}

