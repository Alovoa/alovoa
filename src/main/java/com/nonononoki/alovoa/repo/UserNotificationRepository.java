package com.nonononoki.alovoa.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.UserNotification;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {
	public UserNotification findByUserFromAndUserTo(User userFrom, User userTo);
}

