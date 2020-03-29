package com.nonononoki.alovoa.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.UserDeleteToken;

public interface UserDeleteTokenRepository extends JpaRepository<UserDeleteToken, Long> {

	public UserDeleteToken findByContent(String content);
}

