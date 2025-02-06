package com.nonononoki.alovoa.repo;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.Gender;
import com.nonononoki.alovoa.model.UserSearchRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long> {

    String SEARCH_SELECT_QUERY = "SELECT u FROM User u ";
    String SEARCH_JOIN_MISC_QUERY = "LEFT JOIN u.miscInfos misc ";
    String SEARCH_JOIN_INTEREST_QUERY = "LEFT JOIN u.interests interest ";
    String SEARCH_BASE_QUERY = "WHERE u.disabled = FALSE AND u.admin = FALSE AND u.confirmed = TRUE AND u.intention IS NOT NULL AND "
            + "u.locationLatitude IS NOT NULL AND u.locationLongitude IS NOT NULL "
            + "AND u.profilePicture IS NOT NULL AND :preferedGender MEMBER OF u.preferedGenders "
            + "AND TIMESTAMPDIFF(YEAR, u.dates.dateOfBirth, CURDATE()) + u.preferedMaxAge >= :age AND TIMESTAMPDIFF(YEAR, u.dates.dateOfBirth, CURDATE()) + u.preferedMinAge <= :age AND u.dates.dateOfBirth >= :minDate AND u.dates.dateOfBirth <= :maxDate "
            + "AND u.id NOT IN (:likeIds) AND u.id NOT IN (:likeIds) AND u.id NOT IN (:hideIds) "
            + "AND u.id NOT IN (:blockIds) "
            + "AND u.id NOT IN (:blockedByIds) "
            + "AND u.gender.id IN (:genderIds) ";
    String SEARCH_INTENTION_QUERY = "AND u.intention.id IN (:intentionIds) ";
    String SEARCH_MISC_INFO_QUERY = "AND misc.value IN (:miscInfoIds) ";
    String SEARCH_INTEREST_QUERY = "AND interest.text IN (:interestTexts)  ";
    String SEARCH_LOCATION_QUERY = "AND u.locationLatitude BETWEEN :latitudeFrom AND :latitudeTo AND u.locationLongitude BETWEEN :longitudeFrom AND :longitudeTo ";

    @Query(value = SEARCH_SELECT_QUERY + SEARCH_BASE_QUERY + SEARCH_LOCATION_QUERY)
    List<User> usersSearchBaseQuery(@Param("age") int age, @Param("minDate") Date minDate, @Param("maxDate") Date maxDate,
                                    @Param("preferedGender") Gender preferedGender,
                                    @Param("latitudeFrom") Double latitudeFrom, @Param("latitudeTo") Double latitudeTo,
                                    @Param("longitudeFrom") Double longitudeFrom, @Param("longitudeTo") Double longitudeTo,
                                    @Param("likeIds") Collection<Long> likeIds,
                                    @Param("hideIds") Collection<Long> hideIds, @Param("blockIds") Collection<Long> blockIds,
                                    @Param("blockedByIds") Collection<Long> blockedByIds, @Param("genderIds") Collection<Long> genderIds,
                                    Pageable page);

    @Query(value = SEARCH_SELECT_QUERY + SEARCH_BASE_QUERY + SEARCH_LOCATION_QUERY + SEARCH_INTENTION_QUERY)
    List<User> usersSearchNoExtrasQuery(@Param("age") int age,
                                        @Param("minDate") Date minDate,
                                        @Param("maxDate") Date maxDate,
                                        @Param("preferedGender") Gender preferedGender,
                                        @Param("latitudeFrom") Double latitudeFrom,
                                        @Param("latitudeTo") Double latitudeTo,
                                        @Param("longitudeFrom") Double longitudeFrom,
                                        @Param("longitudeTo") Double longitudeTo,
                                        @Param("likeIds") Collection<Long> likeIds,
                                        @Param("hideIds") Collection<Long> hideIds,
                                        @Param("blockIds") Collection<Long> blockIds,
                                        @Param("blockedByIds") Collection<Long> blockedByIds,
                                        @Param("genderIds") Collection<Long> genderIds,
                                        @Param("intentionIds") Collection<Long> intentionIds,
                                        Pageable page);

    @Query(value = SEARCH_SELECT_QUERY + SEARCH_JOIN_MISC_QUERY + SEARCH_JOIN_INTEREST_QUERY + SEARCH_BASE_QUERY + SEARCH_LOCATION_QUERY + SEARCH_INTENTION_QUERY + SEARCH_MISC_INFO_QUERY + SEARCH_INTEREST_QUERY)
    List<User> usersSearchInterestMiscQuery(@Param("age") int age,
                                            @Param("minDate") Date minDate,
                                            @Param("maxDate") Date maxDate,
                                            @Param("preferedGender") Gender preferedGender,
                                            @Param("latitudeFrom") Double latitudeFrom,
                                            @Param("latitudeTo") Double latitudeTo,
                                            @Param("longitudeFrom") Double longitudeFrom,
                                            @Param("longitudeTo") Double longitudeTo,
                                            @Param("likeIds") Collection<Long> likeIds,
                                            @Param("hideIds") Collection<Long> hideIds,
                                            @Param("blockIds") Collection<Long> blockIds,
                                            @Param("blockedByIds") Collection<Long> blockedByIds,
                                            @Param("genderIds") Collection<Long> genderIds,
                                            @Param("intentionIds") Collection<Long> intentionIds,
                                            @Param("miscInfoIds") Collection<Integer> miscInfos,
                                            @Param("interestTexts") Collection<String> interests,
                                            Pageable page);

    @Query(value = SEARCH_SELECT_QUERY + SEARCH_JOIN_INTEREST_QUERY + SEARCH_BASE_QUERY + SEARCH_LOCATION_QUERY + SEARCH_INTENTION_QUERY + SEARCH_INTEREST_QUERY)
    List<User> usersSearchInterestQuery(@Param("age") int age,
                                        @Param("minDate") Date minDate,
                                        @Param("maxDate") Date maxDate,
                                        @Param("preferedGender") Gender preferedGender,
                                        @Param("latitudeFrom") Double latitudeFrom,
                                        @Param("latitudeTo") Double latitudeTo,
                                        @Param("longitudeFrom") Double longitudeFrom,
                                        @Param("longitudeTo") Double longitudeTo,
                                        @Param("likeIds") Collection<Long> likeIds,
                                        @Param("hideIds") Collection<Long> hideIds,
                                        @Param("blockIds") Collection<Long> blockIds,
                                        @Param("blockedByIds") Collection<Long> blockedByIds,
                                        @Param("genderIds") Collection<Long> genderIds,
                                        @Param("intentionIds") Collection<Long> intentionIds,
                                        @Param("interestTexts") Collection<String> interests,
                                        Pageable page);

    @Query(value = SEARCH_SELECT_QUERY + SEARCH_JOIN_MISC_QUERY + SEARCH_BASE_QUERY + SEARCH_LOCATION_QUERY + SEARCH_INTENTION_QUERY + SEARCH_MISC_INFO_QUERY)
    List<User> usersSearchMiscQuery(@Param("age") int age,
                                    @Param("minDate") Date minDate,
                                    @Param("maxDate") Date maxDate,
                                    @Param("preferedGender") Gender preferedGender,
                                    @Param("latitudeFrom") Double latitudeFrom,
                                    @Param("latitudeTo") Double latitudeTo,
                                    @Param("longitudeFrom") Double longitudeFrom,
                                    @Param("longitudeTo") Double longitudeTo,
                                    @Param("likeIds") Collection<Long> likeIds,
                                    @Param("hideIds") Collection<Long> hideIds,
                                    @Param("blockIds") Collection<Long> blockIds,
                                    @Param("blockedByIds") Collection<Long> blockedByIds,
                                    @Param("genderIds") Collection<Long> genderIds,
                                    @Param("intentionIds") Collection<Long> intentionIds,
                                    @Param("miscInfoIds") Collection<Integer> miscInfos,
                                    Pageable page);

    @Query(value = SEARCH_SELECT_QUERY + SEARCH_BASE_QUERY)
    List<User> usersSearchIgnoreLocation(@Param("age") int age, @Param("minDate") Date minDate, @Param("maxDate") Date maxDate,
                                         @Param("preferedGender") Gender preferedGender, @Param("likeIds") Collection<Long> likeIds,
                                         @Param("hideIds") Collection<Long> hideIds,
                                         @Param("blockIds") Collection<Long> blockIds,
                                         @Param("blockedByIds") Collection<Long> blockedByIds,
                                         @Param("genderIds") Collection<Long> genderIds, Pageable page);

    User findByEmail(String email);

    User findByUuid(UUID uuid);

    long countByConfirmed(boolean confirmed);

    long countByConfirmedAndGenderId(boolean confirmed, long genderId);

    default List<User> usersSearchNoExtras(UserSearchRequest request, Pageable page) {
        return usersSearchNoExtrasQuery(request.getAge(), request.getMinDateDob(), request.getMaxDateDob(), request.getPreferedGender(), request.getMinLat(),
                request.getMaxLat(), request.getMinLong(), request.getMaxLong(),
                request.getLikeIds(), request.getHideIds(), request.getBlockIds(), request.getBlockedByIds(), request.getGenderIds(),
                request.getIntentionIds(),
                page);
    }

    default List<User> usersSearchInterestMisc(UserSearchRequest request, Pageable page) {
        return usersSearchInterestMiscQuery(request.getAge(), request.getMinDateDob(), request.getMaxDateDob(), request.getPreferedGender(),
                request.getMinLat(), request.getMaxLat(), request.getMinLong(), request.getMaxLong(),
                request.getLikeIds(), request.getHideIds(), request.getBlockIds(), request.getBlockedByIds(), request.getGenderIds(),
                request.getIntentionIds(),
                request.getMiscInfos(),
                request.getInterests(),
                page);
    }

    default List<User> usersSearchInterest(UserSearchRequest request, Pageable page) {
        return usersSearchInterestQuery(request.getAge(), request.getMinDateDob(), request.getMaxDateDob(), request.getPreferedGender(),
                request.getMinLat(), request.getMaxLat(), request.getMinLong(), request.getMaxLong(),
                request.getLikeIds(), request.getHideIds(), request.getBlockIds(), request.getBlockedByIds(), request.getGenderIds(),
                request.getIntentionIds(),
                request.getInterests(),
                page);
    }

    default List<User> usersSearchMisc(UserSearchRequest request, Pageable page) {
        return usersSearchMiscQuery(request.getAge(), request.getMinDateDob(), request.getMaxDateDob(), request.getPreferedGender(),
                request.getMinLat(), request.getMaxLat(), request.getMinLong(), request.getMaxLong(),
                request.getLikeIds(), request.getHideIds(), request.getBlockIds(), request.getBlockedByIds(), request.getGenderIds(),
                request.getIntentionIds(),
                request.getMiscInfos(),
                page);
    }

    default List<User> usersBaseSearch(UserSearchRequest request, Pageable page) {
        return usersSearchBaseQuery(request.getAge(), request.getMinDateDob(), request.getMaxDateDob(), request.getPreferedGender(),
                request.getMinLat(), request.getMaxLat(), request.getMinLong(), request.getMaxLong(),
                request.getLikeIds(), request.getHideIds(), request.getBlockIds(), request.getBlockedByIds(), request.getGenderIds(),
                page);
    }

    List<User> findByConfirmedIsFalseAndAdminFalseAndDatesCreationDateBefore(Date date);

    default List<User> usersSearchAllIgnoreLocation(UserSearchRequest request, Pageable page) {
        return usersSearchIgnoreLocation(
                request.getAge(), request.getMinDateDob(), request.getMaxDateDob(), request.getPreferedGender(),
                request.getLikeIds(), request.getHideIds(), request.getBlockIds(),
                request.getBlockedByIds(), request.getGenderIds(), page);
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

    // users donate
    List<User> findTop20ByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullAndProfilePictureNotNullAndDatesDateOfBirthGreaterThanEqualAndDatesDateOfBirthLessThanEqualOrderByTotalDonationsDesc(
            Date minDate, Date maxDate);
}
