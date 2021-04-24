package com.nonononoki.alovoa.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserNotification;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {
	UserNotification findByUserFromAndUserTo(User userFrom, User userTo);
	List<UserNotification> findByUserFrom(User userFrom);
	List<UserNotification> findByUserTo(User userTo);
}

