package com.nonononoki.alovoa.repo;

import com.nonononoki.alovoa.entity.user.Gender;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Set;

public interface GenderRepository extends JpaRepository<Gender, Long> {
	Gender findByText(String text);
}

