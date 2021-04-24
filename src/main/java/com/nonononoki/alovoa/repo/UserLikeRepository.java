package com.nonononoki.alovoa.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserLike;

public interface UserLikeRepository extends JpaRepository<UserLike, Long> {
	UserLike findByUserFromAndUserTo(User userFrom, User userTo);
	List<UserLike> findByUserTo(User userTo);
	List<UserLike> findByUserFrom(User userFrom);
}

