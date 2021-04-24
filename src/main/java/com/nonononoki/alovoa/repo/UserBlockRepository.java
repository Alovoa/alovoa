package com.nonononoki.alovoa.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserBlock;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {
	UserBlock findByUserFromAndUserTo(User userFrom, User userTo);
	List<UserBlock> findByUserFrom(User userFrom);
	List<UserBlock> findByUserTo(User userTo);
}

