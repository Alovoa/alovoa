package com.nonononoki.alovoa.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {
	List<Message> findByUserFrom(User u);
	List<Message> findByUserTo(User u);
}

