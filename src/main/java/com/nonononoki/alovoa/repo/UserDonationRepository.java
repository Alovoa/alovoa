package com.nonononoki.alovoa.repo;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.user.UserDonation;

public interface UserDonationRepository extends JpaRepository<UserDonation, Long> {
	List<UserDonation> findTop20ByUserDisabledFalseAndUserDatesDateOfBirthGreaterThanEqualAndUserDatesDateOfBirthLessThanEqualOrderByDateDesc(Date minDate, Date maxDate);
}

