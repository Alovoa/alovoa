package com.nonononoki.alovoa.repo.matching;

import com.nonononoki.alovoa.entity.matching.FeatureFlagConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeatureFlagConfigRepository extends JpaRepository<FeatureFlagConfig, Long> {

    Optional<FeatureFlagConfig> findByFlagNameAndSegmentKey(String flagName, String segmentKey);
}
