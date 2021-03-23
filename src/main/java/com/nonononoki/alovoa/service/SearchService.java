package com.nonononoki.alovoa.service;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.model.UserSearchRequest;
import com.nonononoki.alovoa.repo.UserRepository;

@Service
public class SearchService {

	public static final int SORT_DISTANCE = 1;
	public static final int SORT_ACTIVE_DATE = 2;
	public static final int SORT_INTEREST = 3;
	public static final int SORT_DONATION = 4;

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

	@Value("${app.age.legal}")
	private int ageLegal;

	@Value("${app.age.min}")
	private int minAge;

	@Value("${app.age.max}")
	private int maxAge;

	private static final double LATITUDE = 111.1;
	private static final double LONGITUDE = 111.320;

	public List<UserDto> search(Double latitude, Double longitude, int distance, int sort) throws Exception {

		if (distance > maxDistance) {
			throw new Exception("max_distance_exceeded");
		}

		User user = authService.getCurrentUser();
		user.getDates().setActiveDate(new Date());
		// rounding to improve privacy
		DecimalFormat df = new DecimalFormat("#.##");
		user.setLocationLatitude(Double.valueOf(df.format(latitude)));
		user.setLocationLongitude(Double.valueOf(df.format(longitude)));
		userRepo.saveAndFlush(user);

		int age = Tools.calcUserAge(user);
		boolean isLegalAge = age >= ageLegal;
		int minAge = user.getPreferedMinAge();
		int maxAge = user.getPreferedMaxAge();

		if (isLegalAge && minAge < this.ageLegal) {
			minAge = this.ageLegal;
		}
		if (!isLegalAge && maxAge >= this.ageLegal) {
			maxAge = this.ageLegal - 1;
		}

		Date minDate = Tools.ageToDate(maxAge);
		Date maxDate = Tools.ageToDate(minAge);

		double deltaLat = distance / LATITUDE;
		double deltaLong = distance / (LONGITUDE * Math.cos(latitude / 180.0 * Math.PI));
		double minLat = latitude - deltaLat;
		double maxLat = latitude + deltaLat;
		double minLong = longitude - deltaLong;
		double maxLong = longitude + deltaLong;

		UserSearchRequest request = UserSearchRequest.builder().minLat(minLat).minLong(minLong).maxLat(maxLat)
				.maxLong(maxLong).maxDate(maxDate).minDate(minDate).intentionText(user.getIntention().getText())
				.likeIds(user.getLikes().stream().map(o -> o.getUserTo().getId()).collect(Collectors.toSet()))
				.blockIds(user.getBlockedUsers().stream().map(o -> o.getUserTo().getId()).collect(Collectors.toSet()))
				.hideIds(user.getHiddenUsers().stream().map(o -> o.getUserTo().getId()).collect(Collectors.toSet()))
				.genderTexts(user.getPreferedGenders().stream().map(o -> o.getText()).collect(Collectors.toSet()))
				.build();

		// because IS IN does not work with empty list
		request.getBlockIds().add(0L);
		request.getLikeIds().add(0L);
		request.getHideIds().add(0L);

		List<User> users = userRepo.usersSearch(request);

		Set<Long> ignoreIds = user.getLikedBy().stream().map(o -> o.getUserFrom().getId()).collect(Collectors.toSet());
		ignoreIds.addAll(
				user.getBlockedByUsers().stream().map(o -> o.getUserFrom().getId()).collect(Collectors.toSet()));
		ignoreIds.add(user.getId());

		List<User> filteredUsers = new ArrayList<User>();

		// filter users
		for (User u : users) {

			if (ignoreIds.contains(u.getId())) {
				continue;
			}

			if (!u.getPreferedGenders().contains(user.getGender())) {
				continue;
			}
			// square is fine, reduces CPU load when not calculating radius distance
			/*
			 * if (dto.getDistanceToUser() > distance) { continue; }
			 */
			filteredUsers.add(u);

			if (filteredUsers.size() >= maxResults) {
				break;
			}
		}

		List<UserDto> userDtos = new ArrayList<>();
		for (User u : filteredUsers) {
			UserDto dto = UserDto.userToUserDto(u, user, textEncryptor, UserDto.PROFILE_PICTURE_ONLY);
			userDtos.add(dto);
		}

		if (sort == SORT_DISTANCE) {
			Collections.sort(userDtos, new Comparator<UserDto>() {
				@Override
				public int compare(UserDto a, UserDto b) {
					return a.getDistanceToUser() < b.getDistanceToUser() ? -1
							: a.getDistanceToUser() > b.getDistanceToUser() ? 1 : 0;
				}
			});
		} else if (sort == SORT_ACTIVE_DATE) {
			Collections.sort(userDtos, new Comparator<UserDto>() {
				@Override
				public int compare(UserDto a, UserDto b) {
					return b.getActiveDate().compareTo(a.getActiveDate());
				}
			});
			Collections.reverse(userDtos);
		} else if (sort == SORT_INTEREST) {
			userDtos.removeIf(f -> f.getSameInterests() == 0);
			Collections.sort(userDtos, new Comparator<UserDto>() {
				@Override
				public int compare(UserDto a, UserDto b) {
					return a.getSameInterests() < b.getSameInterests() ? -1
							: a.getSameInterests() > b.getSameInterests() ? 1 : 0;
				}
			});
		} else if (sort == SORT_DONATION) {
			//userDtos.removeIf(f -> f.getTotalDonations() == 0);
			Collections.sort(userDtos, new Comparator<UserDto>() {
				@Override
				public int compare(UserDto a, UserDto b) {
					return a.getTotalDonations() < b.getTotalDonations() ? -1
							: a.getTotalDonations() > b.getTotalDonations() ? 1 : 0;
				}
			});
		}

		return userDtos;
	}

}
