package com.nonononoki.alovoa.repo;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.UserSearchRequest;

public interface UserRepository extends JpaRepository<User, Long> {

	public static final int MAX_USERS_SEARCH = 200;

	User findByEmail(String email);

	long countByConfirmed(boolean confirmed);

	long countByConfirmedAndGenderId(boolean confirmed, long genderId);

	List<User> findByZodiacNull();

	default List<User> usersSearch(UserSearchRequest request, Sort sort) {
		return findTop200ByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullAndProfilePictureNotNullAndDatesDateOfBirthGreaterThanEqualAndDatesDateOfBirthLessThanEqualAndLocationLatitudeBetweenAndLocationLongitudeBetweenAndIntentionTextEqualsAndIdNotInAndIdNotInAndIdNotInAndGenderTextIn(
				request.getMinDate(), request.getMaxDate(), request.getMinLat(), request.getMaxLat(),
				request.getMinLong(), request.getMaxLong(), request.getIntentionText(), request.getLikeIds(),
				request.getHideIds(), request.getBlockIds(), request.getGenderTexts(), sort);
	}

	default List<User> usersSearchAll(UserSearchRequest request, Sort sort) {
		return findByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullAndProfilePictureNotNullAndDatesDateOfBirthGreaterThanEqualAndDatesDateOfBirthLessThanEqualAndLocationLatitudeBetweenAndLocationLongitudeBetweenAndIntentionTextEqualsAndIdNotInAndIdNotInAndIdNotInAndGenderTextIn(
				request.getMinDate(), request.getMaxDate(), request.getMinLat(), request.getMaxLat(),
				request.getMinLong(), request.getMaxLong(), request.getIntentionText(), request.getLikeIds(),
				request.getHideIds(), request.getBlockIds(), request.getGenderTexts(), sort);
	}

	default List<User> usersSearchAllIgnoreLocation(UserSearchRequest request, Sort sort) {
		return findTop200ByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullAndProfilePictureNotNullAndDatesDateOfBirthGreaterThanEqualAndDatesDateOfBirthLessThanEqualAndIntentionTextEqualsAndIdNotInAndIdNotInAndIdNotInAndGenderTextIn(
				request.getMinDate(), request.getMaxDate(), request.getIntentionText(), request.getLikeIds(),
				request.getHideIds(), request.getBlockIds(), request.getGenderTexts(), sort);
	}

	default List<User> usersSearchAllIgnoreLocationAndIntention(UserSearchRequest request, Sort sort) {
		return findTop200ByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullAndProfilePictureNotNullAndDatesDateOfBirthGreaterThanEqualAndDatesDateOfBirthLessThanEqualAndIdNotInAndIdNotInAndIdNotInAndGenderTextIn(
				request.getMinDate(), request.getMaxDate(), request.getLikeIds(), request.getHideIds(),
				request.getBlockIds(), request.getGenderTexts(), sort);
	}

	// almost all, must have complete profile and not blocked
	default List<User> usersSearchAllIgnoreAll(UserSearchRequest request, Sort sort) {
		return findTop50ByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullAndProfilePictureNotNullAndDatesDateOfBirthGreaterThanEqualAndDatesDateOfBirthLessThanEqualAndIdNotInAndIdNotInAndIdNotIn(
				request.getMinDate(), request.getMaxDate(), request.getLikeIds(), request.getHideIds(),
				request.getBlockIds(), sort);
	}

	default List<User> usersDonate(Date minDate, Date maxDate) {
		return findTop20ByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullAndProfilePictureNotNullAndDatesDateOfBirthGreaterThanEqualAndDatesDateOfBirthLessThanEqualOrderByTotalDonationsDesc(
				minDate, maxDate);
	}

	default List<User> adminSearch() {
		return findTop100ByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullAndProfilePictureNotNullOrderByDatesCreationDateDesc();
	}

	// used for sending mails to all
	List<User> findByDisabledFalseAndAdminFalseAndConfirmedTrue();

	List<User> findTop100ByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullAndProfilePictureNotNullOrderByDatesCreationDateDesc();

	List<User> findTop200ByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullAndProfilePictureNotNullAndDatesDateOfBirthGreaterThanEqualAndDatesDateOfBirthLessThanEqualAndLocationLatitudeBetweenAndLocationLongitudeBetweenAndIntentionTextEqualsAndIdNotInAndIdNotInAndIdNotInAndGenderTextIn(
			Date min, Date max, Double latitudeFrom, Double latitudeTo, Double longitudeFrom, Double longitudeTo,
			String intentionText, Collection<Long> likeIds, Collection<Long> hideIds, Collection<Long> blockIds,
			Collection<String> genderTexts, Sort sort);

	List<User> findByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullAndProfilePictureNotNullAndDatesDateOfBirthGreaterThanEqualAndDatesDateOfBirthLessThanEqualAndLocationLatitudeBetweenAndLocationLongitudeBetweenAndIntentionTextEqualsAndIdNotInAndIdNotInAndIdNotInAndGenderTextIn(
			Date min, Date max, Double latitudeFrom, Double latitudeTo, Double longitudeFrom, Double longitudeTo,
			String intentionText, Collection<Long> likeIds, Collection<Long> hideIds, Collection<Long> blockIds,
			Collection<String> genderTexts, Sort sort);

	List<User> findTop20ByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullAndProfilePictureNotNullAndDatesDateOfBirthGreaterThanEqualAndDatesDateOfBirthLessThanEqualOrderByTotalDonationsDesc(
			Date minDate, Date maxDate);

	List<User> findTop200ByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullAndProfilePictureNotNullAndDatesDateOfBirthGreaterThanEqualAndDatesDateOfBirthLessThanEqualAndIntentionTextEqualsAndIdNotInAndIdNotInAndIdNotInAndGenderTextIn(
			Date min, Date max, String intentionText, Collection<Long> likeIds, Collection<Long> hideIds,
			Collection<Long> blockIds, Collection<String> genderTexts, Sort sort);

	List<User> findTop200ByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullAndProfilePictureNotNullAndDatesDateOfBirthGreaterThanEqualAndDatesDateOfBirthLessThanEqualAndIdNotInAndIdNotInAndIdNotInAndGenderTextIn(
			Date min, Date max, Collection<Long> likeIds, Collection<Long> hideIds, Collection<Long> blockIds,
			Collection<String> genderTexts, Sort sort);

	List<User> findTop50ByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullAndProfilePictureNotNullAndDatesDateOfBirthGreaterThanEqualAndDatesDateOfBirthLessThanEqualAndIdNotInAndIdNotInAndIdNotIn(
			Date min, Date max, Collection<Long> likeIds, Collection<Long> hideIds, Collection<Long> blockIds,
			Sort sort);
}
