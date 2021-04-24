package com.nonononoki.alovoa.repo;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.user.UserRegisterToken;

public interface UserRegisterTokenRepository extends JpaRepository<UserRegisterToken, Long> {
	UserRegisterToken findByContent(String content);
	List<UserRegisterToken> findByDateBefore(Date date);
}

