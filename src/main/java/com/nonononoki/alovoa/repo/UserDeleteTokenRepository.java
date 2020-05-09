package com.nonononoki.alovoa.repo;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.UserDeleteToken;

public interface UserDeleteTokenRepository extends JpaRepository<UserDeleteToken, Long> {

	public List<UserDeleteToken> findByDateBefore(Date date);
}

