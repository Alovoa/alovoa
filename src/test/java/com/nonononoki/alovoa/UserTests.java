package com.nonononoki.alovoa;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

	@Transactional
	@Test
	public void test() throws Exception {

		// one default admin user
		Assert.assertEquals(userRepo.count(), 1);

		// register and confirm test users
		Captcha c1 = captchaService.generate();
		RegisterDto user1Dto = createTestUserDto(1, c1);
		String tokenContent1 = registerService.register(user1Dto);
		User user1 = registerService.registerConfirm(tokenContent1);

		Captcha c2 = captchaService.generate();
		RegisterDto user2Dto = createTestUserDto(2, c2);
		String tokenContent2 = registerService.register(user2Dto);
		User user2 = registerService.registerConfirm(tokenContent2);

		Captcha c3 = captchaService.generate();
		RegisterDto user3Dto = createTestUserDto(2, c3);
		String tokenContent3 = registerService.register(user3Dto);
		User user3 = registerService.registerConfirm(tokenContent3);

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

		// setup settings
		Mockito.when(authService.getCurrentUser()).thenReturn(user1);
		String img1 = Tools.imageToB64(Tools.getFileFromResources("img/profile1.png"));
		userService.updateProfilePicture(img1);
		userService.addInterest("interest1");
		userService.addImage(img1);
		userService.updateDescription("description1");
		userService.updateIntention(1);
		userService.updateMaxAge(100);
		userService.updateMinAge(16);
		userService.updatePreferedGender(2, true);
		userService.updateTheme(1);

		Mockito.when(authService.getCurrentUser()).thenReturn(user2);
		String img2 = Tools.imageToB64(Tools.getFileFromResources("img/profile2.png"));
		userService.updateProfilePicture(img2);
		userService.addInterest("interest2");
		userService.addImage(img2);
		userService.updateDescription("description2");
		userService.updateIntention(1);
		userService.updateMaxAge(100);
		userService.updateMinAge(16);
		userService.updatePreferedGender(1, true);
		userService.updateTheme(1);

		Mockito.when(authService.getCurrentUser()).thenReturn(user3);
		String img3 = Tools.imageToB64(Tools.getFileFromResources("img/profile3.png"));
		userService.updateProfilePicture(img3);
		userService.addInterest("interest3");
		userService.addImage(img3);
		userService.updateDescription("description3");
		userService.updateIntention(1);
		userService.updateMaxAge(100);
		userService.updateMinAge(16);
		userService.updatePreferedGender(1, true);
		userService.updateTheme(1);

		Mockito.when(authService.getCurrentUser()).thenReturn(user1);
		List<UserDto> searchDtos1 = searchService.search("0", "0", 50, 1);
		Assert.assertEquals(searchDtos1.size(), 2);

		Mockito.when(authService.getCurrentUser()).thenReturn(user2);
		List<UserDto> searchDtos2 = searchService.search("0", "0", 50, 1);
		Assert.assertEquals(searchDtos2.size(), 1);

		Mockito.when(authService.getCurrentUser()).thenReturn(user3);
		List<UserDto> searchDtos3 = searchService.search("0", "0", 50, 1);
		Assert.assertEquals(searchDtos3.size(), 1);

	}

	private RegisterDto createTestUserDto(long gender, Captcha c) throws IOException {

		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.YEAR, minAge * (-1));
		RegisterDto dto = new RegisterDto();
		dto.setEmail(RandomStringUtils.randomAlphanumeric(10) + "@mailinator.com");
		dto.setDateOfBirth(calendar.getTime());
		dto.setPassword("test123");
		dto.setFirstName("test");
		dto.setGender(gender);
		dto.setIntention(0);
		dto.setCaptchaId(c.getId());
		dto.setCaptchaText(c.getText());
		dto.setTermsConditions(true);
		dto.setPrivacy(true);
		return dto;
	}
}
