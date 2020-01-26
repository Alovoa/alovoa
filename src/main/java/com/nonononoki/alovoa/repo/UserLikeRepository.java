package com.nonononoki.alovoa.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.UserLike;

public interface UserLikeRepository extends JpaRepository<UserLike, Long> {
	public UserLike findByUserFromAndUserTo(User userFrom, User userTo);
}

