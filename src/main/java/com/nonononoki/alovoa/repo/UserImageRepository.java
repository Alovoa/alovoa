package com.nonononoki.alovoa.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.user.UserImage;

public interface UserImageRepository extends JpaRepository<UserImage, Long> {
}

