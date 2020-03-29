package com.nonononoki.alovoa.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.Conversation;
import com.nonononoki.alovoa.entity.User;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
	public List<Conversation> findAllByUserTo(User u);
	public List<Conversation> findAllByUserFrom(User u);
}

