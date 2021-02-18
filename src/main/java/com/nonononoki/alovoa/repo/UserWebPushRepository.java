package com.nonononoki.alovoa.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.user.UserWebPush;

public interface UserWebPushRepository extends JpaRepository<UserWebPush, Long> {
	List<UserWebPush> findAllByIp(String ip);
}