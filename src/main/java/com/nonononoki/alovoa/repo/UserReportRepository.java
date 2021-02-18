package com.nonononoki.alovoa.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserReport;

public interface UserReportRepository extends JpaRepository<UserReport, Long> {
	public UserReport findByUserFromAndUserTo(User userFrom, User userTo);
	public List<UserReport> findTop20ByOrderByDateAsc();
}