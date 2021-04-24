package com.nonononoki.alovoa.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.user.UserInterest;

public interface UserInterestRepository extends JpaRepository<UserInterest, Long> {	
}

