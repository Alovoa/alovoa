package com.nonononoki.alovoa.repo;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserHide;

public interface UserHideRepository extends JpaRepository<UserHide, Long> {
	UserHide findByUserFromAndUserTo(User userFrom, User userTo);
	List<UserHide> findByUserFrom(User userFrom);
	List<UserHide> findByUserTo(User userTo);
	
	@Query(value = "SELECT h FROM UserHide h WHERE h.date < (:date)")
	List<UserHide> findByByDateBefore(@Param("date") Date date, Pageable page);
}

