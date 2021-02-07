package com.nonononoki.alovoa;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import com.nonononoki.alovoa.entity.Captcha;
import com.nonononoki.alovoa.entity.Location;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.RegisterDto;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.service.AuthService;
import com.nonononoki.alovoa.service.CaptchaService;
import com.nonononoki.alovoa.service.RegisterService;
import com.nonononoki.alovoa.service.SearchService;
import com.nonononoki.alovoa.service.UserService;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration
@ActiveProfiles("test")
@Service
public class UserTests {

	@Autowired
	private RegisterService registerService;

	@Autowired
	private CaptchaService captchaService;

	@Autowired
	private UserService userService;

	@Autowired
	private SearchService searchService;

	@Autowired
	private UserRepository userRepo;

	@Value("${app.age.min}")
	private int minAge;

	@MockBean
	private AuthService authService;

	private final int INTENTION_TEST = 1;

	private List<User> testUsers = null;

	@Transactional
	public void registerTestUsers() throws Exception {
		// register and confirm test users
		Captcha c1 = captchaService.generate();
		RegisterDto user1Dto = createTestUserDto(1, c1, "test1");
		String tokenContent1 = registerService.register(user1Dto);
		User user1 = registerService.registerConfirm(tokenContent1);

		Captcha c2 = captchaService.generate();
		RegisterDto user2Dto = createTestUserDto(2, c2, "test2");
		String tokenContent2 = registerService.register(user2Dto);
		User user2 = registerService.registerConfirm(tokenContent2);

		Captcha c3 = captchaService.generate();
		RegisterDto user3Dto = createTestUserDto(2, c3, "test3");
		String tokenContent3 = registerService.register(user3Dto);
		User user3 = registerService.registerConfirm(tokenContent3);

		testUsers = new ArrayList<>();
		testUsers.add(user1);
		testUsers.add(user2);
		testUsers.add(user3);
	}

	private RegisterDto createTestUserDto(long gender, Captcha c, String email) throws IOException {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.YEAR, minAge * (-1));
		RegisterDto dto = new RegisterDto();
		dto.setEmail(email + "@mailinator.com");
		dto.setDateOfBirth(calendar.getTime());
		dto.setPassword("test123");
		dto.setFirstName("test");
		dto.setGender(gender);
		dto.setCaptchaId(c.getId());
		dto.setCaptchaText(c.getText());
		dto.setTermsConditions(true);
		dto.setPrivacy(true);
		return dto;
	}

	@Transactional
	@Test
	public void test() throws Exception {

		// one default admin user
		Assert.assertEquals(userRepo.count(), 1);

		registerTestUsers();
		User user1 = testUsers.get(0);
		User user2 = testUsers.get(1);
		User user3 = testUsers.get(2);

		// set location manually since no extra service is needed
		Location loc1 = new Location();
		loc1.setLatitude("0");
		loc1.setLongitude("0");
		user1.setLastLocation(loc1);

		Location loc2 = new Location();
		loc2.setLatitude("0");
		loc2.setLongitude("0");
		loc2.setUser(user2);
		user2.setLastLocation(loc2);

		Location loc3 = new Location();
		loc3.setLatitude("0");
		loc3.setLongitude("0");
		loc3.setUser(user3);
		user3.setLastLocation(loc3);

		userRepo.saveAndFlush(user1);
		userRepo.saveAndFlush(user2);
		userRepo.saveAndFlush(user3);

		Assert.assertEquals(userRepo.count(), 4);

		String imgMime = "png";
		// setup settings
		Mockito.when(authService.getCurrentUser()).thenReturn(user1);
		String img1 = Tools.imageToB64(Tools.getFileFromResources("img/profile1.png"), imgMime);
		userService.updateProfilePicture(img1);
		userService.addInterest("interest1");
		userService.updateDescription("description1");
		userService.updateIntention(INTENTION_TEST);
		userService.updateMaxAge(100);
		userService.updateMinAge(16);
		userService.updatePreferedGender(2, true);

		Mockito.when(authService.getCurrentUser()).thenReturn(user2);
		String img2 = Tools.imageToB64(Tools.getFileFromResources("img/profile2.png"), imgMime);
		userService.updateProfilePicture(img2);
		userService.addInterest("interest1");
		userService.updateDescription("description2");
		userService.updateIntention(INTENTION_TEST);
		userService.updateMaxAge(100);
		userService.updateMinAge(16);
		userService.updatePreferedGender(1, true);

		Mockito.when(authService.getCurrentUser()).thenReturn(user3);
		String img3 = Tools.imageToB64(Tools.getFileFromResources("img/profile3.png"), imgMime);
		userService.updateProfilePicture(img3);
		Assert.assertTrue("profile_picture", user3.getProfilePicture() != null);
		userService.addInterest("interest1");
		Assert.assertTrue("interest", user3.getInterests().size() == 1);
		String description = "description3";
		userService.updateDescription(description);
		Assert.assertTrue("description", user3.getDescription().equals(description));
		userService.updateIntention(INTENTION_TEST);
		Assert.assertTrue("intention", user3.getIntention().getId() == INTENTION_TEST);
		int maxAge = 100;
		userService.updateMaxAge(maxAge);
		Assert.assertTrue("max_age", user3.getPreferedMaxAge() == maxAge);
		int minAge = 16;
		userService.updateMinAge(minAge);
		Assert.assertTrue("min_age", user3.getPreferedMinAge() == minAge);
		userService.updatePreferedGender(1, true);
		//TODO Assert PREF_GENDER
		userService.deleteInterest(authService.getCurrentUser().getInterests().get(0).getId());
		Assert.assertTrue("interest", user3.getInterests().size() == 0);
		userService.addInterest("interest1");
		int theme = 1;
		userService.updateTheme(theme);
		Assert.assertTrue("theme", user3.getTheme() == theme);
		userService.addImage(img3);
		Assert.assertTrue("image", user3.getImages().size() == 1);
		userService.deleteImage(authService.getCurrentUser().getImages().get(0).getId());
		Assert.assertTrue("image", user3.getImages().size() == 0);
		userService.deleteProfilePicture();
		Assert.assertTrue("profile_picture", user3.getProfilePicture() == null);
		userService.updateProfilePicture(img3);
		Assert.assertTrue("profile_picture", user3.getProfilePicture() != null);
		userService.updateAudio(Tools.resourceToB64("audio/file_example_MP3_700KB.mp3"));
		Assert.assertTrue("audio", user3.getAudio() != null);
		userService.deleteAudio();
		Assert.assertTrue("audio", user3.getAudio() == null);
		
		searchTest(user1, user2, user3);
	}
	
	private void searchTest(User user1, User user2, User user3) throws Exception {
		Mockito.when(authService.getCurrentUser()).thenReturn(user1);
		List<UserDto> searchDtos1 = searchService.search("0", "0", 50, 1);
		Assert.assertEquals(searchDtos1.size(), 2);

		Mockito.when(authService.getCurrentUser()).thenReturn(user2);
		List<UserDto> searchDtos2 = searchService.search("0", "0", 50, 1);
		Assert.assertEquals(searchDtos2.size(), 1);

		Mockito.when(authService.getCurrentUser()).thenReturn(user3);
		List<UserDto> searchDtos3 = searchService.search("0", "0", 50, 1);
		Assert.assertEquals(searchDtos3.size(), 1);
		
		//TODO Check distance

		// TODO likeUser
		// TODO hideUser
		// TODO blockUser
		
	}
}
