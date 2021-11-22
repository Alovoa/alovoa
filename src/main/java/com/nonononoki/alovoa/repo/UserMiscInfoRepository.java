package com.nonononoki.alovoa.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.user.UserMiscInfo;

public interface UserMiscInfoRepository extends JpaRepository<UserMiscInfo, Long> {
	UserMiscInfo findByValue(long value);
}

