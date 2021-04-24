package com.nonononoki.alovoa.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.user.UserDates;

public interface UserDatesRepository extends JpaRepository<UserDates, Long> {
}

