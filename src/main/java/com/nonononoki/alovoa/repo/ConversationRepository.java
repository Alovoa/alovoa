package com.nonononoki.alovoa.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.user.Conversation;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
	List<Conversation> findByUsers_Id(long userId);
}

