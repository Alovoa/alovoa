package com.nonononoki.alovoa.repo;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.UserSearchRequest;

public interface UserRepository extends JpaRepository<User, Long> {

	public User findByEmail(String email);

	default List<User> usersSearch(UserSearchRequest request) {

		return findTop200ByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullAndDatesDateOfBirthGreaterThanEqualAndDatesDateOfBirthLessThanEqualAndLocationLatitudeBetweenAndLocationLongitudeBetweenAndIntentionTextEqualsAndIdNotInAndIdNotInAndIdNotInAndGenderTextIn(
				request.getMinDate(), request.getMaxDate(), request.getMinLat(), request.getMaxLat(),
				request.getMinLong(), request.getMaxLong(), request.getIntentionText(), request.getLikeIds(),
				request.getHideIds(), request.getBlockIds(), request.getGenderTexts());
	}
	
	default List<User> usersSearchAll(UserSearchRequest request) {

		return findByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullAndDatesDateOfBirthGreaterThanEqualAndDatesDateOfBirthLessThanEqualAndLocationLatitudeBetweenAndLocationLongitudeBetweenAndIntentionTextEqualsAndIdNotInAndIdNotInAndIdNotInAndGenderTextIn(
				request.getMinDate(), request.getMaxDate(), request.getMinLat(), request.getMaxLat(),
				request.getMinLong(), request.getMaxLong(), request.getIntentionText(), request.getLikeIds(),
				request.getHideIds(), request.getBlockIds(), request.getGenderTexts());
	}

	default List<User> usersDonate() {
		return findTop100ByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullOrderByTotalDonationsDesc();
	}

	public List<User> findByDisabledFalseAndAdminFalseAndConfirmedTrue();

	public List<User> findTop200ByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullAndDatesDateOfBirthGreaterThanEqualAndDatesDateOfBirthLessThanEqualAndLocationLatitudeBetweenAndLocationLongitudeBetweenAndIntentionTextEqualsAndIdNotInAndIdNotInAndIdNotInAndGenderTextIn(
			Date min, Date max, Double latitudeFrom, Double latitudeTo, Double longitudeFrom, Double longitudeTo,
			String intentionText, Collection<Long> likeIds, Collection<Long> hideIds, Collection<Long> blockIds,
			Collection<String> genderTexts);
	
	public List<User> findByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullAndDatesDateOfBirthGreaterThanEqualAndDatesDateOfBirthLessThanEqualAndLocationLatitudeBetweenAndLocationLongitudeBetweenAndIntentionTextEqualsAndIdNotInAndIdNotInAndIdNotInAndGenderTextIn(
			Date min, Date max, Double latitudeFrom, Double latitudeTo, Double longitudeFrom, Double longitudeTo,
			String intentionText, Collection<Long> likeIds, Collection<Long> hideIds, Collection<Long> blockIds,
			Collection<String> genderTexts);

	public List<User> findTop100ByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullOrderByTotalDonationsDesc();
}
