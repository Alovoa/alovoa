package com.nonononoki.alovoa.repo;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.UserHide;

public interface UserHideRepository extends JpaRepository<UserHide, Long> {
	public UserHide findByUserFromAndUserTo(User userFrom, User userTo);
	public List<UserHide> findByDateBefore(Date date);
}

