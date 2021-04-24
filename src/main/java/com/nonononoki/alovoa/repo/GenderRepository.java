package com.nonononoki.alovoa.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.user.Gender;

public interface GenderRepository extends JpaRepository<Gender, Long> {
	Gender findByText(String text);
}

