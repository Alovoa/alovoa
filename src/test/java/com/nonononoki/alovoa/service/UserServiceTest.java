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
public class UserServiceTest {

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

	private final int INTENTION_TEST = 1;

	private final String INTEREST = "interest";
	
	private List<User> testUsers;
	
	@BeforeEach
	public void before() throws Exception {
		testUsers = RegisterServiceTest.getTestUsers(captchaService, registerService, firstNameLengthMax, firstNameLengthMin);
	}
	
	@AfterEach
	public void after() throws Exception {
		RegisterServiceTest.deleteAllUsers(userService, authService, captchaService, conversationRepo, userRepo);
	}

	@Test
	public void test() throws Exception {

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

		Assert.assertEquals(userRepo.count(), 4);

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
		Assert.assertTrue("profile_picture", user3.getProfilePicture() != null);
		userService.addInterest(INTEREST);
		Assert.assertTrue("interest", user3.getInterests().size() == 1);
		String description = "description3";
		userService.updateDescription(description);
		Assert.assertTrue("description", user3.getDescription().equals(description));
		userService.updateIntention(INTENTION_TEST);
		Assert.assertTrue("intention", user3.getIntention().getId() == INTENTION_TEST);
		userService.updateMaxAge(maxAge);
		Assert.assertTrue("max_age", user3.getPreferedMaxAge() == maxAge);

		userService.updateMinAge(minAge);
		Assert.assertTrue("min_age", user3.getPreferedMinAge() == minAge);
		userService.updatePreferedGender(1, true);
		Assert.assertTrue(user3.getPreferedGenders().size() == 1);
		userService.updatePreferedGender(2, true);
		Assert.assertTrue(user3.getPreferedGenders().size() == 2);
		userService.updatePreferedGender(2, false);
		Assert.assertTrue(user3.getPreferedGenders().size() == 1);

		userService.deleteInterest(authService.getCurrentUser().getInterests().get(0).getId());
		Assert.assertTrue("interest", authService.getCurrentUser().getInterests().size() == 0);
		userService.addInterest(INTEREST);
		userService.addImage(img3);
		Assert.assertTrue("image", authService.getCurrentUser().getImages().size() == 1);
		userService.deleteImage(authService.getCurrentUser().getImages().get(0).getId());
		Assert.assertTrue("image", authService.getCurrentUser().getImages().size() == 0);
		userService.deleteProfilePicture();
		Assert.assertTrue("profile_picture", authService.getCurrentUser().getProfilePicture() == null);
		userService.updateProfilePicture(img3);
		Assert.assertTrue("profile_picture", authService.getCurrentUser().getProfilePicture() != null);
		userService.updateAudio(Tools.resourceToB64("audio/file_example_MP3_700KB.mp3"), "mpeg");
		Assert.assertTrue("audio", user3.getAudio() != null);
		userService.deleteAudio();
		Assert.assertTrue("audio", user3.getAudio() == null);

		Assert.assertThrows(Exception.class, () -> {
			deleteTest(user1);
		});

		// USERDATA
		ResponseEntity<Resource> userData = userService.getUserdata();
		InputStream inputStream = ((ByteArrayResource) userData.getBody()).getInputStream();
		String userDataString = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()
				.collect(Collectors.joining("\n"));
		UserGdpr gdpr = objectMapper.readValue(userDataString, UserGdpr.class);
		Assert.assertTrue(gdpr.getDescription().equals(authService.getCurrentUser().getDescription()));
		Assert.assertTrue(gdpr.getEmail().equals(authService.getCurrentUser().getEmail()));
		Assert.assertTrue(gdpr.getFirstName().equals(authService.getCurrentUser().getFirstName()));
		// Assert.assertTrue(gdpr.getDates().equals(authService.getCurrentUser().getDates()));
		Assert.assertTrue(gdpr.getDonations().equals(authService.getCurrentUser().getDonations()));
		// Assert.assertTrue(gdpr.getGender().equals(authService.getCurrentUser().getGender()));
		// Assert.assertTrue(gdpr.getIntention().equals(authService.getCurrentUser().getIntention()));
		Assert.assertTrue(gdpr.getInterests().equals(authService.getCurrentUser().getInterests()));
		Assert.assertTrue(gdpr.getLocationLatitude().equals(authService.getCurrentUser().getLocationLatitude()));
		Assert.assertTrue(gdpr.getLocationLongitude().equals(authService.getCurrentUser().getLocationLongitude()));
		Assert.assertTrue(gdpr.getMessageSent().equals(authService.getCurrentUser().getMessageSent()));
		Assert.assertTrue(gdpr.getNumberProfileViews() == authService.getCurrentUser().getNumberProfileViews());
		Assert.assertTrue(gdpr.getNumberSearches() == authService.getCurrentUser().getNumberSearches());
		// Assert.assertTrue(gdpr.getPreferedGenders().equals(authService.getCurrentUser().getPreferedGenders()));
		Assert.assertTrue(gdpr.getPreferedMaxAge() == (authService.getCurrentUser().getPreferedMaxAge()));
		Assert.assertTrue(gdpr.getTotalDonations() == (authService.getCurrentUser().getTotalDonations()));
		Assert.assertTrue(gdpr.getWebPush().equals(authService.getCurrentUser().getWebPush()));
		
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
