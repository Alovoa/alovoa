package com.nonononoki.alovoa.repo.matching;

import com.nonononoki.alovoa.entity.matching.UserRollingStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRollingStatsRepository extends JpaRepository<UserRollingStats, Long> {

    Optional<UserRollingStats> findByUserIdAndSegmentKey(Long userId, String segmentKey);

    List<UserRollingStats> findByUserIdInAndSegmentKey(Collection<Long> userIds, String segmentKey);
}
