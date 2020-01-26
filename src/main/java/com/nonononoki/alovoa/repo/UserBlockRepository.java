package com.nonononoki.alovoa.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.UserBlock;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {
	public UserBlock findByUserFromAndUserTo(User userFrom, User userTo);
}

