package com.nonononoki.alovoa.repo;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.UserSearchRequest;

public interface UserRepository extends JpaRepository<User, Long> {

	User findByEmail(String email);

	long countByConfirmed(boolean confirmed);

	long countByConfirmedAndGenderId(boolean confirmed, long genderId);

	default List<User> usersSearch(UserSearchRequest request, Pageable page) {
		return usersSearchQuery(request.getAge(), request.getMinDateDob(), request.getMaxDateDob(), request.getMinLat(),
				request.getMaxLat(), request.getMinLong(), request.getMaxLong(), request.getIntentionId(),
				request.getLikeIds(), request.getHideIds(), request.getBlockIds(), request.getGenderIds(), page);
	}

	@Query(value = "SELECT u FROM User u WHERE u.disabled = FALSE AND u.admin = FALSE AND u.confirmed = TRUE AND u.intention IS NOT NULL AND "
			+ "u.locationLatitude IS NOT NULL AND u.locationLongitude IS NOT NULL AND u.profilePicture IS NOT NULL "
			+ "AND TIMESTAMPDIFF(YEAR, u.dates.dateOfBirth, CURDATE()) + u.preferedMaxAge >= :age AND TIMESTAMPDIFF(YEAR, u.dates.dateOfBirth, CURDATE()) + u.preferedMinAge <= :age AND u.dates.dateOfBirth >= :minDate AND u.dates.dateOfBirth <= :maxDate "
			+ "AND u.locationLatitude BETWEEN :latitudeFrom AND :latitudeTo AND u.locationLongitude BETWEEN :longitudeFrom AND :longitudeTo "
			+ "AND u.intention.id = :intentionId AND u.id NOT IN (:likeIds) AND u.id NOT IN (:likeIds) AND u.id NOT IN (:hideIds) "
			+ "AND u.id NOT IN (:blockIds) AND u.gender.id IN (:genderIds)")
	List<User> usersSearchQuery(@Param("age") int age, @Param("minDate") Date minDate, @Param("maxDate") Date maxDate,
			@Param("latitudeFrom") Double latitudeFrom, @Param("latitudeTo") Double latitudeTo,
			@Param("longitudeFrom") Double longitudeFrom, @Param("longitudeTo") Double longitudeTo,
			@Param("intentionId") long intentionId, @Param("likeIds") Collection<Long> likeIds,
			@Param("hideIds") Collection<Long> hideIds, @Param("blockIds") Collection<Long> blockIds,
			@Param("genderIds") Collection<Long> genderIds, Pageable page);

	default List<User> usersSearchAllIgnoreLocation(UserSearchRequest request, Pageable page) {
		return usersSearchIgnoreLocation(
				request.getAge(), request.getMinDateDob(), request.getMaxDateDob(),
				request.getIntentionId(), request.getLikeIds(), request.getHideIds(), request.getBlockIds(),
				request.getGenderIds(), page);
	}
	
	@Query(value = "SELECT u FROM User u WHERE u.disabled = FALSE AND u.admin = FALSE AND u.confirmed = TRUE AND u.intention IS NOT NULL AND "
			+ "u.locationLatitude IS NOT NULL AND u.locationLongitude IS NOT NULL AND u.profilePicture IS NOT NULL "
			+ "AND TIMESTAMPDIFF(YEAR, u.dates.dateOfBirth, CURDATE()) + u.preferedMaxAge >= :age AND TIMESTAMPDIFF(YEAR, u.dates.dateOfBirth, CURDATE()) + u.preferedMinAge <= :age AND u.dates.dateOfBirth >= :minDate AND u.dates.dateOfBirth <= :maxDate "
			+ "AND u.intention.id = :intentionId AND u.id NOT IN (:likeIds) AND u.id NOT IN (:likeIds) AND u.id NOT IN (:hideIds) "
			+ "AND u.id NOT IN (:blockIds) AND u.gender.id IN (:genderIds)")
	List<User> usersSearchIgnoreLocation(@Param("age") int age, @Param("minDate") Date minDate, @Param("maxDate") Date maxDate,
			@Param("intentionId") long intentionId, @Param("likeIds") Collection<Long> likeIds,
			@Param("hideIds") Collection<Long> hideIds, @Param("blockIds") Collection<Long> blockIds,
			@Param("genderIds") Collection<Long> genderIds, Pageable page);

	default List<User> usersSearchAllIgnoreLocationAndIntention(UserSearchRequest request, Sort sort) {
		return findTop200FilteredUsers(
				request.getMinDateDob(), request.getMaxDateDob(), request.getLikeIds(), request.getHideIds(),
				request.getBlockIds(), request.getGenderIds(), sort);
	}

	// almost all, must have complete profile and not blocked
	default List<User> usersSearchAllIgnoreAll(UserSearchRequest request, Sort sort) {
		return findTop50ActiveUsersWithProfileIntentionInRangeAndExcludedIds(
				request.getMinDateDob(), request.getMaxDateDob(), request.getLikeIds(), request.getHideIds(),
				request.getBlockIds(), sort);
	}

	default List<User> usersDonate(Date minDate, Date maxDate) {
		return findTop20ActiveDonors(minDate, maxDate);
	}

	default List<User> adminSearch() {
		return findTop100ActiveUsersOrderedByCreationDate();
	}

	// used for sending mails to all
	List<User> findByDisabledFalseAndAdminFalseAndConfirmedTrue();

	List<User> findTop100ActiveUsersOrderedByCreationDate();

	List<User> findTop200FilteredUsers(Date min, Date max, Collection<Long> likeIds, Collection<Long> hideIds, Collection<Long> blockIds, Collection<Long> genderIds, Sort sort);

	List<User> findTop50ActiveUsersWithProfileIntentionInRangeAndExcludedIds(Date min, Date max, Collection<Long> likeIds, Collection<Long> hideIds, Collection<Long> blockIds, Sort sort);

	// users donate
	List<User> findTop20ActiveDonors(Date minDate, Date maxDate);
}
