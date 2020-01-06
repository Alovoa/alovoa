package com.nonononoki.alovoa.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.Conversation;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

}

