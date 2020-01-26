package com.nonononoki.alovoa.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.UserHide;

public interface UserHideRepository extends JpaRepository<UserHide, Long> {
	public UserHide findByUserFromAndUserTo(User userFrom, User userTo);
}

