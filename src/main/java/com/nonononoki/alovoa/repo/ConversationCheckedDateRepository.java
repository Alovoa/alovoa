package com.nonononoki.alovoa.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.user.Conversation;
import com.nonononoki.alovoa.entity.user.ConversationCheckedDate;

public interface ConversationCheckedDateRepository extends JpaRepository<ConversationCheckedDate, Long> {
	public List<ConversationCheckedDate> findByUserId(long id);
	public List<ConversationCheckedDate> findByConversationAndUserId(Conversation c, long userId);
}

