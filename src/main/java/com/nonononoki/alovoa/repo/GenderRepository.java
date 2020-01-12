package com.nonononoki.alovoa.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.Gender;

public interface GenderRepository extends JpaRepository<Gender, Long> {
	
	public Gender findByText(String text);
}

