package com.nonononoki.alovoa.repo;

import com.nonononoki.alovoa.entity.user.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {
}
