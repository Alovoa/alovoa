package com.nonononoki.alovoa.repo;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.user.UserPasswordToken;

public interface UserPasswordTokenRepository extends JpaRepository<UserPasswordToken, Long> {
	public UserPasswordToken findByContent(String content);

	public List<UserPasswordToken> findByDateBefore(Date date);
}

