package com.nonononoki.alovoa.repo.matching;

import com.nonononoki.alovoa.entity.matching.UserVisualAttractiveness;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserVisualAttractivenessRepository extends JpaRepository<UserVisualAttractiveness, Long> {

    Optional<UserVisualAttractiveness> findByUserId(Long userId);

    List<UserVisualAttractiveness> findByUserIdIn(Collection<Long> userIds);
}
