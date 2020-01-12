package com.nonononoki.alovoa.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.UserInterest;

public interface UserInterestRepository extends JpaRepository<UserInterest, Long> {
	
	public UserInterest findByText(String text);
}

