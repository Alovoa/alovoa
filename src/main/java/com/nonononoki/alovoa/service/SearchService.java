package com.nonononoki.alovoa.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.entity.Location;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.repo.UserRepository;

@Service
public class SearchService {
	
	private final int SORT_DISTANCE = 1;
	private final int SORT_ACTIVE_DATE = 2;

	@Autowired
	private AuthService authService;
	
	@Autowired
	private UserRepository userRepo;
	
	@SuppressWarnings("unlikely-arg-type")
	public List<UserDto> search(String latitude, String longitude, int distance, int sort) throws Exception {
		User user = authService.getCurrentUser();
		user.setActiveDate(new Date());
		Location loc = new Location();
		loc.setLatitude(latitude);
		loc.setLongitude(longitude);
		user.setLastLocation(loc);
		userRepo.save(user);
		
		List<User> users = userRepo.findByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLastLocationNotNullAndAgeBetween(user.getPreferedMinAge(), user.getPreferedMaxAge());
		List<UserDto> userDtos = new ArrayList<>();
		for(int i = 0; i < users.size(); i++) {
			userDtos.add(userToUserDto(users.get(i), user));
		}
		
		//filter users
		List<UserDto> filteredUserDtos = new ArrayList<>();
		for(int i = 0; i < userDtos.size(); i++) {
			UserDto dto = userDtos.get(i);
			if(user.getHiddenUsers().contains(dto)) {
				continue;
			}		
			if(user.getBlockedUsers().contains(dto)) {
				continue;
			}
			if(!user.getPreferedGenders().contains(dto.getGender())) {
				continue;
			}
			if(!dto.getPreferedGenders().contains(user.getGender())) {
				continue;
			}
			if(!user.getIntention().equals(dto.getIntention())) {
				continue;
			}
			if(dto.getDistanceToUser() > distance) {
				continue;
			}
			filteredUserDtos.add(dto);
		}
		
		if(sort == SORT_DISTANCE) {
			Collections.sort(filteredUserDtos,new Comparator<UserDto>() {
			    @Override
			    public int compare(UserDto a, UserDto b) {
			    	return a.getDistanceToUser() < b.getDistanceToUser() ? -1
			    	         : a.getDistanceToUser() > b.getDistanceToUser() ? 1
			    	         : 0;
			    }
			});
		} else if(sort == SORT_ACTIVE_DATE) {
			Collections.sort(filteredUserDtos,new Comparator<UserDto>() {
			    @Override
			    public int compare(UserDto a, UserDto b) {
			    	return a.getActiveDate().compareTo(b.getActiveDate());
			    }
			});
			Collections.reverse(filteredUserDtos);
		}
		return filteredUserDtos;
	}
	
	private UserDto userToUserDto(User user, User currentUser) {
		UserDto dto = new UserDto();
		dto.setActiveDate(user.getActiveDate());
		dto.setAge(user.getAge());
		dto.setDescription(user.getDescription());
		dto.setFirstName(user.getFirstName());
		dto.setGender(user.getGender());
		dto.setPreferedGenders(user.getPreferedGenders());
		dto.setImages(user.getImages());
		dto.setGender(user.getGender());
		dto.setIntention(user.getIntention());
		dto.setProfilePicture(user.getProfilePicture());
		dto.setNumberOfReports(user.getReportedByUsers().size());
		dto.setNumberOfBlocks(user.getBlockedByUsers().size());
		double donations = 0;
		for(int i = 0; i < user.getDonations().size(); i++) {
			donations += user.getDonations().get(i).getAmount();
		}
		dto.setTotalDonations(donations);	
		dto.setDistanceToUser(getDistanceToUser(user, currentUser));
		return dto;
	}
	
	
	private double getDistanceToUser(User user, User currUser) {
		return calcDistance(Double.parseDouble(user.getLastLocation().getLatitude()),
						Double.parseDouble(user.getLastLocation().getLongitude()),
						Double.parseDouble(currUser.getLastLocation().getLatitude()),
						Double.parseDouble(currUser.getLastLocation().getLongitude())
				);
	}
	
	//https://stackoverflow.com/questions/3694380/calculating-distance-between-two-points-using-latitude-longitude/20410612#20410612
	private double calcDistance(double lat1, double lng1, double lat2, double lng2) {
		double a = (lat1 - lat2) * distPerLat(lat1);
		double b = (lng1 - lng2) * distPerLng(lat1);
		return Math.sqrt(a * a + b * b);
	}
	private double distPerLng(double lat) {
		return 0.0003121092 * Math.pow(lat, 4) + 0.0101182384 * Math.pow(lat, 3) - 17.2385140059 * lat * lat
				+ 5.5485277537 * lat + 111301.967182595;
	}
	private double distPerLat(double lat) {
		return -0.000000487305676 * Math.pow(lat, 4) - 0.0033668574 * Math.pow(lat, 3) + 0.4601181791 * lat * lat
				- 1.4558127346 * lat + 110579.25662316;
	}
	
}
