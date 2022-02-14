package com.nonononoki.alovoa.repo;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.user.UserDeleteToken;

public interface UserDeleteTokenRepository extends JpaRepository<UserDeleteToken, Long> {
	List<UserDeleteToken> findTop100ByDateAfter(Date d);
}

