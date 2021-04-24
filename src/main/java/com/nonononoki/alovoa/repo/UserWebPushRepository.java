package com.nonononoki.alovoa.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.user.UserWebPush;

public interface UserWebPushRepository extends JpaRepository<UserWebPush, Long> {
}