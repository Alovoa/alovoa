package com.nonononoki.alovoa.repo;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserHide;

public interface UserHideRepository extends JpaRepository<UserHide, Long> {
	UserHide findByUserFromAndUserTo(User userFrom, User userTo);
	List<UserHide> findByUserFrom(User userFrom);
	List<UserHide> findByUserTo(User userTo);
	List<UserHide> findTop100ByDateBefore(Date date);
}

