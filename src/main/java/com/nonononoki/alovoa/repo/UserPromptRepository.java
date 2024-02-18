package com.nonononoki.alovoa.repo;

import com.nonononoki.alovoa.entity.user.UserPrompt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPromptRepository extends JpaRepository<UserPrompt, Long> {
}

