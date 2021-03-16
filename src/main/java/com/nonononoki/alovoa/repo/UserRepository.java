package com.nonononoki.alovoa.repo;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.service.SearchService;

public interface UserRepository extends JpaRepository<User, Long> {

	public User findByEmail(String email);
	
	default List<User> usersSearch(LocalDate min, LocalDate max, SearchService.MinMaxLatLong minMaxLatLong) {
		return findByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullAndDatesDateOfBirthGreaterThanEqualAndDatesDateOfBirthLessThanEqualAndLocationLatitudeBetweenAndLocationLongitudeBetween(
				min, max, minMaxLatLong.getMinLat(), minMaxLatLong.getMaxLat(), minMaxLatLong.getMinLon(), minMaxLatLong.getMaxLon());
	}
	
	default List<User> usersDonate() {
		return findByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullOrderByTotalDonationsDesc();
	}
	
	public List<User> findByDisabledFalseAndAdminFalseAndConfirmedTrue();

	public List<User> findByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullAndDatesDateOfBirthGreaterThanEqualAndDatesDateOfBirthLessThanEqualAndLocationLatitudeBetweenAndLocationLongitudeBetween(
			LocalDate min, LocalDate max, Double latitudeFrom, Double latitudeTo, Double longitudeFrom, Double longitudeTo);

	public List<User> findByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLocationLatitudeNotNullOrderByTotalDonationsDesc();
}
