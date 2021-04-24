package com.nonononoki.alovoa.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.user.UserIntention;

public interface UserIntentionRepository extends JpaRepository<UserIntention, Long> {
	UserIntention findByText(String text);
}

