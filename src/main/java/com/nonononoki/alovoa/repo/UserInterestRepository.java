package com.nonononoki.alovoa.repo;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nonononoki.alovoa.entity.user.UserInterest;
import com.nonononoki.alovoa.model.UserInterestDto;

public interface UserInterestRepository extends JpaRepository<UserInterest, Long> {

	@Query(value = "SELECT NEW com.nonononoki.alovoa.model.UserInterestDto(i.text, COUNT(*)) FROM UserInterest i WHERE i.text NOT IN (:interests) AND i.text LIKE (:name) GROUP BY i.text ORDER BY COUNT(*) DESC")
	List<UserInterestDto> getInterestAutocomplete(@Param("name") String name,
			@Param("interests") List<String> interests, Pageable page);
}
