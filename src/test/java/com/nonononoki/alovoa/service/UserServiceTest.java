package com.nonononoki.alovoa.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.entity.Captcha;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserDeleteToken;
import com.nonononoki.alovoa.model.UserDeleteAccountDto;
import com.nonononoki.alovoa.model.UserGdpr;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceTest {

	@Autowired
	private RegisterService registerService;

	@Autowired
	private CaptchaService captchaService;

	@Autowired
	private UserService userService;

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private ConversationRepository conversationRepo;

	@Autowired
	private ObjectMapper objectMapper;

	@Value("${app.age.min}")
	private int minAge;

	@Value("${app.age.max}")
	private int maxAge;

	@Value("${app.message.size}")
	private int maxMessageSize;

	@Value("${app.conversation.messages-max}")
	private int maxConvoMessages;

	@Value("${app.first-name.length-max}")
	private int firstNameLengthMax;

	@Value("${app.first-name.length-min}")
	private int firstNameLengthMin;

	@MockBean
	private AuthService authService;

	private final Long INTENTION_TEST = 1L;

	private final String INTEREST = "interest";
	
	private List<User> testUsers;
	
	@BeforeEach
	void before() throws Exception {
		testUsers = RegisterServiceTest.getTestUsers(captchaService, registerService, firstNameLengthMax, firstNameLengthMin);
	}
	
	@AfterEach
	void after() throws Exception {
		RegisterServiceTest.deleteAllUsers(userService, authService, captchaService, conversationRepo, userRepo);
	}

	@Test
	void test() throws Exception {

		User user1 = testUsers.get(0);
		User user2 = testUsers.get(1);
		User user3 = testUsers.get(2);

		// set location manually since no extra service is needed
		user1.setLocationLatitude(0.0);
		user1.setLocationLongitude(0.0);

		user2.setLocationLatitude(0.0);
		user2.setLocationLongitude(0.0);

		user3.setLocationLatitude(0.0);
		user3.setLocationLongitude(0.0);

		userRepo.saveAndFlush(user1);
		userRepo.saveAndFlush(user2);
		userRepo.saveAndFlush(user3);

		Assert.assertEquals(4, userRepo.count());

		String imgMimePng = "png";
		// setup settings
		Mockito.when(authService.getCurrentUser()).thenReturn(user1);
		String img1 = Tools.imageToB64("img/profile1.png", imgMimePng);
		userService.updateProfilePicture(img1);
		userService.addInterest(INTEREST);
		userService.updateDescription("description1");
		userService.updateIntention(INTENTION_TEST);
		userService.updateMaxAge(100);
		userService.updateMinAge(16);
		userService.updatePreferedGender(2, true);

		// check for urls
		{
			Assert.assertThrows(Exception.class, () -> {
				userService.updateDescription("hidden url example.com");
			});

			Assert.assertThrows(Exception.class, () -> {
				userService.updateDescription("hidden url test.example.com");
			});

			Assert.assertThrows(Exception.class, () -> {
				userService.updateDescription("hidden url test.bit.ly/fdsfasdgadrsfgafgfdsaf13");
			});

			Assert.assertThrows(Exception.class, () -> {
				userService.updateDescription("hidden email test.test@test.com");
			});

			Assert.assertThrows(Exception.class, () -> {
				userService.updateDescription("hidden email test.test+1234@test.net");
			});
		}

		Mockito.when(authService.getCurrentUser()).thenReturn(user2);
		String img2 = Tools.imageToB64("img/profile2.png", imgMimePng);
		userService.updateProfilePicture(img2);
		userService.addInterest(INTEREST);
		userService.updateDescription("description2");
		userService.updateIntention(INTENTION_TEST);
		userService.updateMaxAge(100);
		userService.updateMinAge(16);
		userService.updatePreferedGender(1, true);

		Mockito.when(authService.getCurrentUser()).thenReturn(user3);
		String img3 = Tools.imageToB64("img/profile3.png", imgMimePng);
		userService.updateProfilePicture(img3);
		Assert.assertNotNull(user3.getProfilePicture());
		userService.addInterest(INTEREST);
		Assert.assertEquals(1, user3.getInterests().size());
		String description = "description3";
		userService.updateDescription(description);
		Assert.assertEquals(description, user3.getDescription());
		userService.updateIntention(INTENTION_TEST);
		Assert.assertEquals(INTENTION_TEST, user3.getIntention().getId());
		userService.updateMaxAge(maxAge);
		Assert.assertEquals(maxAge, user3.getPreferedMaxAge());

		userService.updateMinAge(minAge);
		Assert.assertEquals(minAge, user3.getPreferedMinAge());
		userService.updatePreferedGender(1, true);
		Assert.assertEquals(1, user3.getPreferedGenders().size());
		userService.updatePreferedGender(2, true);
		Assert.assertEquals(2, user3.getPreferedGenders().size());
		userService.updatePreferedGender(2, false);
		Assert.assertEquals(1, user3.getPreferedGenders().size());

		userService.deleteInterest(authService.getCurrentUser().getInterests().get(0).getId());
		Assert.assertEquals(0, authService.getCurrentUser().getInterests().size());
		userService.addInterest(INTEREST);
		userService.addImage(img3);
		Assert.assertEquals(1, authService.getCurrentUser().getImages().size());
		userService.deleteImage(authService.getCurrentUser().getImages().get(0).getId());
		Assert.assertEquals(0, authService.getCurrentUser().getImages().size());
		userService.deleteProfilePicture();
		Assert.assertNotNull( authService.getCurrentUser().getProfilePicture());
		userService.updateProfilePicture(img3);
		Assert.assertNotNull(authService.getCurrentUser().getProfilePicture());
		userService.updateAudio(Tools.resourceToB64("audio/file_example_MP3_700KB.mp3"), "mpeg");
		Assert.assertNotNull(user3.getAudio());
		userService.deleteAudio();
		Assert.assertNotNull(user3.getAudio());

		Assert.assertThrows(Exception.class, () -> {
			deleteTest(user1);
		});

		// USERDATA
		ResponseEntity<Resource> userData = userService.getUserdata();
		InputStream inputStream = ((ByteArrayResource) userData.getBody()).getInputStream();
		String userDataString = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()
				.collect(Collectors.joining("\n"));
		UserGdpr gdpr = objectMapper.readValue(userDataString, UserGdpr.class);
		Assert.assertEquals(authService.getCurrentUser().getDescription(), gdpr.getDescription());
		Assert.assertEquals(authService.getCurrentUser().getEmail(), gdpr.getEmail());
		Assert.assertEquals(authService.getCurrentUser().getFirstName(), gdpr.getFirstName());
		// Assert.assertEquals(gdpr.getDates().equals(authService.getCurrentUser().getDates()));
		Assert.assertEquals(authService.getCurrentUser().getDonations(), gdpr.getDonations());
		// Assert.assertEquals(gdpr.getGender().equals(authService.getCurrentUser().getGender()));
		// Assert.assertEquals(gdpr.getIntention().equals(authService.getCurrentUser().getIntention()));
		Assert.assertEquals(authService.getCurrentUser().getInterests(), gdpr.getInterests());
		Assert.assertEquals(authService.getCurrentUser().getLocationLatitude(), gdpr.getLocationLatitude());
		Assert.assertEquals(authService.getCurrentUser().getLocationLongitude(), gdpr.getLocationLongitude());
		Assert.assertEquals(authService.getCurrentUser().getMessageSent(), gdpr.getMessageSent());
		Assert.assertEquals(authService.getCurrentUser().getNumberProfileViews(), gdpr.getNumberProfileViews());
		Assert.assertEquals(authService.getCurrentUser().getNumberSearches(), gdpr.getNumberSearches());
		// Assert.assertEquals(gdpr.getPreferedGenders().equals(authService.getCurrentUser().getPreferedGenders()));
		Assert.assertEquals(authService.getCurrentUser().getPreferedMaxAge(), gdpr.getPreferedMaxAge());
		Assert.assertEquals(authService.getCurrentUser().getTotalDonations(), gdpr.getTotalDonations(), 0.001);
		Assert.assertEquals(authService.getCurrentUser().getWebPush(), gdpr.getWebPush());
		
	}

	private void deleteTest(User user) throws Exception {
		UserDeleteToken token = userService.deleteAccountRequest();
		UserDeleteAccountDto dto = new UserDeleteAccountDto();
		Captcha captcha = captchaService.generate();
		dto.setCaptchaId(captcha.getId());
		dto.setCaptchaText(captcha.getText());
		dto.setConfirm(true);
		dto.setEmail(user.getEmail());
		dto.setTokenString(token.getContent());
		userService.deleteAccountConfirm(dto);
	}
}
