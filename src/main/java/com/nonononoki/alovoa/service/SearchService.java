package com.nonononoki.alovoa.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.Location;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.repo.UserRepository;

@Service
public class SearchService {

	private final int SORT_DISTANCE = 1;
	private final int SORT_ACTIVE_DATE = 2;
	private final int SORT_INTEREST = 3;
	private final int SORT_DONATION = 4;
	
	@Autowired
	private TextEncryptorConverter textEncryptor;

	@Autowired
	private AuthService authService;

	@Autowired
	private UserRepository userRepo;

	@Value("${app.search.max}")
	private int maxResults;
	
	@Value("${app.search.max.distance}")
	private int maxDistance;

	public List<UserDto> search(String latitude, String longitude, int distance, int sort) throws Exception {
		
		if(distance > maxDistance) {
			throw new Exception("");
		}
		
		User user = authService.getCurrentUser();
		user.getDates().setActiveDate(new Date());
		Location loc = new Location();
		loc.setLatitude(latitude);
		loc.setLongitude(longitude);
		user.setLastLocation(loc);
		userRepo.saveAndFlush(user);
		
		LocalDate minDate = LocalDate.now().minusYears(user.getPreferedMaxAge());
		LocalDate maxDate = LocalDate.now().minusYears(user.getPreferedMinAge());

		List<User> users = userRepo
				.findByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLastLocationNotNullAndDatesDateOfBirthGreaterThanEqualAndDatesDateOfBirthLessThanEqual(
						minDate, maxDate);
		int count = users.size();
		List<UserDto> userDtos = new ArrayList<>();
		for (int i = 0; i < users.size(); i++) {
			UserDto dto = UserDto.userToUserDto(users.get(i), user, textEncryptor);
			userDtos.add(dto);
		}

		// filter users
		List<UserDto> filteredUserDtos = new ArrayList<>();
		for (int i = 0; i < userDtos.size(); i++) {
			UserDto dto = userDtos.get(i);
			if (user.getId() == dto.getId()) {
				continue;
			}
			if (user.getLikes().stream().anyMatch(o -> o.getUserTo().getId().equals(dto.getId()))) {
				continue;
			}
			if (user.getHiddenUsers().stream().anyMatch(o -> o.getUserTo().getId().equals(dto.getId()))) {
				continue;
			}
			if (user.getBlockedUsers().stream().anyMatch(o -> o.getUserTo().getId().equals(dto.getId()))) {
				continue;
			}
			if (dto.getBlockedUsers().stream().anyMatch(o -> o.getUserTo().getId().equals(user.getId()))) {
				continue;
			}
			if (!user.getPreferedGenders().contains(dto.getGender())) {
				continue;
			}
			if (!dto.getPreferedGenders().contains(user.getGender())) {
				continue;
			}
			if (!user.getIntention().equals(dto.getIntention())) {
				continue;
			}
			if (dto.getDistanceToUser() > distance) {
				continue;
			}
			filteredUserDtos.add(dto);
		}

		if (sort == SORT_DISTANCE) {
			Collections.sort(filteredUserDtos, new Comparator<UserDto>() {
				@Override
				public int compare(UserDto a, UserDto b) {
					return a.getDistanceToUser() < b.getDistanceToUser() ? -1
							: a.getDistanceToUser() > b.getDistanceToUser() ? 1 : 0;
				}
			});
		} else if (sort == SORT_ACTIVE_DATE) {
			Collections.sort(filteredUserDtos, new Comparator<UserDto>() {
				@Override
				public int compare(UserDto a, UserDto b) {
					return b.getActiveDate().compareTo(a.getActiveDate());
				}
			});
			Collections.reverse(filteredUserDtos);
		} else if (sort == SORT_INTEREST) {
			filteredUserDtos.removeIf(f->f.getSameInterests() == 0);
			Collections.sort(filteredUserDtos, new Comparator<UserDto>() {
				@Override
				public int compare(UserDto a, UserDto b) {
					return a.getSameInterests() < b.getSameInterests() ? -1
							: a.getSameInterests() > b.getSameInterests() ? 1 : 0;
				}
			});
		} else if (sort == SORT_DONATION) {
			filteredUserDtos.removeIf(f->f.getTotalDonations() == 0);
			Collections.sort(filteredUserDtos, new Comparator<UserDto>() {
				@Override
				public int compare(UserDto a, UserDto b) {
					return a.getTotalDonations() < b.getTotalDonations() ? -1
							: a.getTotalDonations() > b.getTotalDonations() ? 1 : 0;
				}
			});
		}

		filteredUserDtos = filteredUserDtos.stream().limit(maxResults).collect(Collectors.toList());

		return filteredUserDtos;
	}

}
