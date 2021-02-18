package com.nonononoki.alovoa.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.user.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {
	
}

