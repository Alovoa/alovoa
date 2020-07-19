package com.nonononoki.alovoa.repo;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

	public User findByEmail(String email);

	public List<User> findByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLastLocationNotNullAndDatesDateOfBirthGreaterThanEqualAndDatesDateOfBirthLessThanEqual(
			LocalDate min, LocalDate max);

	public List<User> findByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLastLocationNotNullOrderByTotalDonationsDesc();
}
