package com.nonononoki.alovoa.repo;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.UserSearchRequest;

public interface UserRepository extends JpaRepository<User, Long> {

	public static final int MAX_USERS_SEARCH = 200;

	User findByEmail(String email);

	default List<User> usersSearch(UserSearchRequest request) {
		return findTop200ByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullAndProfilePictureNotNullAndDatesDateOfBirthGreaterThanEqualAndDatesDateOfBirthLessThanEqualAndLocationLatitudeBetweenAndLocationLongitudeBetweenAndIntentionTextEqualsAndIdNotInAndIdNotInAndIdNotInAndGenderTextIn(
				request.getMinDate(), request.getMaxDate(), request.getMinLat(), request.getMaxLat(),
				request.getMinLong(), request.getMaxLong(), request.getIntentionText(), request.getLikeIds(),
				request.getHideIds(), request.getBlockIds(), request.getGenderTexts());
	}

	default List<User> usersSearchAll(UserSearchRequest request) {
		return findByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullAndProfilePictureNotNullAndDatesDateOfBirthGreaterThanEqualAndDatesDateOfBirthLessThanEqualAndLocationLatitudeBetweenAndLocationLongitudeBetweenAndIntentionTextEqualsAndIdNotInAndIdNotInAndIdNotInAndGenderTextIn(
				request.getMinDate(), request.getMaxDate(), request.getMinLat(), request.getMaxLat(),
				request.getMinLong(), request.getMaxLong(), request.getIntentionText(), request.getLikeIds(),
				request.getHideIds(), request.getBlockIds(), request.getGenderTexts());
	}

	default List<User> usersSearchAllIgnoreLocation(UserSearchRequest request) {
		return findTop200ByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullAndProfilePictureNotNullAndDatesDateOfBirthGreaterThanEqualAndDatesDateOfBirthLessThanEqualAndIntentionTextEqualsAndIdNotInAndIdNotInAndIdNotInAndGenderTextIn(
				request.getMinDate(), request.getMaxDate(), request.getIntentionText(), request.getLikeIds(),
				request.getHideIds(), request.getBlockIds(), request.getGenderTexts());
	}
	
	default List<User> usersSearchAllIgnoreLocationAndIntention(UserSearchRequest request) {
		return findTop200ByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullAndProfilePictureNotNullAndDatesDateOfBirthGreaterThanEqualAndDatesDateOfBirthLessThanEqualAndIdNotInAndIdNotInAndIdNotInAndGenderTextIn(
				request.getMinDate(), request.getMaxDate(), request.getLikeIds(),
				request.getHideIds(), request.getBlockIds(), request.getGenderTexts());
	}

	//almost all, must have complete profile and not blocked
	default List<User> usersSearchAllIgnoreAll(UserSearchRequest request) {
		return findTop50ByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullAndProfilePictureNotNullAndDatesDateOfBirthGreaterThanEqualAndDatesDateOfBirthLessThanEqualAndIdNotInAndIdNotInAndIdNotIn(
				request.getMinDate(), request.getMaxDate(), request.getLikeIds(), request.getHideIds(), request.getBlockIds());
	}

	default List<User> usersDonate() {
		return findTop20ByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullAndProfilePictureNotNullOrderByTotalDonationsDesc();
	}

	List<User> findByDisabledFalseAndAdminFalseAndConfirmedTrue();

	List<User> findTop200ByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullAndProfilePictureNotNullAndDatesDateOfBirthGreaterThanEqualAndDatesDateOfBirthLessThanEqualAndLocationLatitudeBetweenAndLocationLongitudeBetweenAndIntentionTextEqualsAndIdNotInAndIdNotInAndIdNotInAndGenderTextIn(
			Date min, Date max, Double latitudeFrom, Double latitudeTo, Double longitudeFrom, Double longitudeTo,
			String intentionText, Collection<Long> likeIds, Collection<Long> hideIds, Collection<Long> blockIds,
			Collection<String> genderTexts);

	List<User> findByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullAndProfilePictureNotNullAndDatesDateOfBirthGreaterThanEqualAndDatesDateOfBirthLessThanEqualAndLocationLatitudeBetweenAndLocationLongitudeBetweenAndIntentionTextEqualsAndIdNotInAndIdNotInAndIdNotInAndGenderTextIn(
			Date min, Date max, Double latitudeFrom, Double latitudeTo, Double longitudeFrom, Double longitudeTo,
			String intentionText, Collection<Long> likeIds, Collection<Long> hideIds, Collection<Long> blockIds,
			Collection<String> genderTexts);

	List<User> findTop20ByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullAndProfilePictureNotNullOrderByTotalDonationsDesc();

	List<User> findTop200ByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullAndProfilePictureNotNullAndDatesDateOfBirthGreaterThanEqualAndDatesDateOfBirthLessThanEqualAndIntentionTextEqualsAndIdNotInAndIdNotInAndIdNotInAndGenderTextIn(
			Date min, Date max, String intentionText, Collection<Long> likeIds, Collection<Long> hideIds,
			Collection<Long> blockIds, Collection<String> genderTexts);
	
	List<User> findTop200ByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullAndProfilePictureNotNullAndDatesDateOfBirthGreaterThanEqualAndDatesDateOfBirthLessThanEqualAndIdNotInAndIdNotInAndIdNotInAndGenderTextIn(
			Date min, Date max, Collection<Long> likeIds, Collection<Long> hideIds,
			Collection<Long> blockIds, Collection<String> genderTexts);

	List<User> findTop50ByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullAndProfilePictureNotNullAndDatesDateOfBirthGreaterThanEqualAndDatesDateOfBirthLessThanEqualAndIdNotInAndIdNotInAndIdNotIn(
			Date min, Date max, Collection<Long> likeIds, Collection<Long> hideIds, Collection<Long> blockIds);
}
