package com.nonononoki.alovoa.service;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.entity.Gender;
import com.nonononoki.alovoa.entity.Location;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.UserIntention;
import com.nonononoki.alovoa.repo.GenderRepository;
import com.nonononoki.alovoa.repo.UserIntentionRepository;
import com.nonononoki.alovoa.repo.UserRepository;

@Service
public class AdminService {

	@Autowired
	private AuthService authService;

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private GenderRepository genderRepo;

	@Autowired
	private UserIntentionRepository intentRepo;

	// create users close to the admin
	public void addTestUsers(String latitude, String longitude) throws Exception {
		User user = authService.getCurrentUser();
		if (!user.isAdmin()) {
			throw new Exception("");
		}
		
		double lat = Double.parseDouble(latitude);
		//double lon = Double.parseDouble(longitude);
		// Length in meters of 1° of latitude = always 111.32 km
		//approx. latitude per km
		final double KM_LAT = 0.009;
		
		// create 5 test users
		User u = new User();
		Location loc = new Location();
		loc.setLongitude(longitude);
		Gender genderFemale = genderRepo.findByText("female");
		Gender genderMale = genderRepo.findByText("male");
		Set<Gender> preferedGenders = new HashSet<Gender>();
		preferedGenders.add(genderMale);
		UserIntention datingIntention = intentRepo.findByText("date");
		
		u.getDates().setCreationDate(new Date());
		u.getDates().setActiveDate(new Date());
		//u.setAge(18);
		u.setConfirmed(true);
		u.setDescription("");
		u.setGender(genderFemale);
		u.setPreferedGenders(preferedGenders);
		u.setIntention(datingIntention);
		u.setLastLocation(loc);
		u.setPreferedMinAge(16);
		u.setPreferedMaxAge(30);
		u.setPassword("test");
		
		u.setId(null);
		loc.setLatitude(String.valueOf(lat + KM_LAT));
		u.setFirstName("Bertha");
		u.setEmail("test5001@test.com");
		u.setProfilePicture(Tools.imageToB64(Tools.getFileFromResources("img/profile2.png")));
		userRepo.saveAndFlush(u);
	}

}
