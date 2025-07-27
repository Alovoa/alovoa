package com.nonononoki.alovoa.repo;

import com.nonononoki.alovoa.entity.user.UserAudio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserAudioRepository extends JpaRepository<UserAudio, Long> {
    //@Cacheable("UserAudioRepository.findByUuid")
    UserAudio findByUuid(UUID uuid);
}

