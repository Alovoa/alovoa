package com.nonononoki.alovoa.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.UserRegisterToken;

public interface UserRegisterTokenRepository extends JpaRepository<UserRegisterToken, Long> {

	public UserRegisterToken findByContent(String content);
}

