package com.nonononoki.alovoa.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.UserReport;

public interface UserReportRepository extends JpaRepository<UserReport, Long> {
	public UserReport findByUserFromAndUserTo(User userFrom, User userTo);
}

