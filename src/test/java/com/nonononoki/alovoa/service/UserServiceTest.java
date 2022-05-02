package com.nonononoki.alovoa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

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
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.Captcha;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserDeleteToken;
import com.nonononoki.alovoa.model.UserDeleteAccountDto;
import com.nonononoki.alovoa.model.UserDto;
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

	@Autowired
	private TextEncryptorConverter textEncryptor;

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

	@MockBean
	private MailService mailService;

	private final Long INTENTION_TEST = 1L;

	private final String INTEREST = "interest";

	private List<User> testUsers;

	@BeforeEach
	void before() throws Exception {
		Mockito.when(mailService.sendMail(Mockito.any(String.class), any(String.class), any(String.class),
				any(String.class))).thenReturn(true);
		testUsers = RegisterServiceTest.getTestUsers(captchaService, registerService, firstNameLengthMax,
				firstNameLengthMin);
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

		assertEquals(4, userRepo.count());

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
			assertThrows(Exception.class, () -> {
				userService.updateDescription("hidden url example.com");
			});

			assertThrows(Exception.class, () -> {
				userService.updateDescription("hidden url test.example.com");
			});

			assertThrows(Exception.class, () -> {
				userService.updateDescription("hidden url test.bit.ly/fdsfasdgadrsfgafgfdsaf13");
			});

			assertThrows(Exception.class, () -> {
				userService.updateDescription("hidden email test.test@test.com");
			});

			assertThrows(Exception.class, () -> {
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
		assertNotNull(user3.getProfilePicture());
		userService.addInterest(INTEREST);
		assertEquals(1, user3.getInterests().size());
		String description = "description3";
		userService.updateDescription(description);
		assertEquals(description, user3.getDescription());
		userService.updateIntention(INTENTION_TEST);
		assertEquals(INTENTION_TEST, user3.getIntention().getId());
		userService.updateMaxAge(maxAge);
		assertEquals(maxAge, user3.getPreferedMaxAge());

		userService.updateMinAge(minAge);
		assertEquals(minAge, user3.getPreferedMinAge());
		userService.updatePreferedGender(1, true);
		assertEquals(3, user3.getPreferedGenders().size());
		userService.updatePreferedGender(2, true);
		assertEquals(3, user3.getPreferedGenders().size());
		userService.updatePreferedGender(2, false);
		assertEquals(2, user3.getPreferedGenders().size());

		userService.deleteInterest(authService.getCurrentUser().getInterests().get(0).getId());
		assertEquals(0, authService.getCurrentUser().getInterests().size());
		userService.addInterest(INTEREST);
		userService.addImage(img3);
		assertEquals(1, authService.getCurrentUser().getImages().size());
		userService.deleteImage(authService.getCurrentUser().getImages().get(0).getId());
		assertEquals(0, authService.getCurrentUser().getImages().size());
		userService.deleteProfilePicture();
		assertNull(authService.getCurrentUser().getProfilePicture());
		userService.updateProfilePicture(img3);
		assertNotNull(authService.getCurrentUser().getProfilePicture());
		userService.updateAudio(Tools.resourceToB64("audio/file_example_MP3_700KB.mp3"), "mpeg");
		assertNotNull(user3.getAudio());
		userService.deleteAudio();
		assertNull(user3.getAudio());

		assertThrows(Exception.class, () -> {
			deleteTest(user1);
		});
		
		
		//PROFILE PICTURE
		String imgLong = Tools.imageToB64("img/long.jpeg", imgMimePng);
		userService.updateProfilePicture(imgLong);
		String imgWide = Tools.imageToB64("img/wide.webp", imgMimePng);
		userService.updateProfilePicture(imgWide);

		// USERDATA
		Mockito.when(authService.getCurrentUser()).thenReturn(user1);
		String idEnc = UserDto.encodeId(user1.getId(), textEncryptor);
		ResponseEntity<Resource> userData = userService.getUserdata(idEnc);
		InputStream inputStream = ((ByteArrayResource) userData.getBody()).getInputStream();
		String userDataString = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()
				.collect(Collectors.joining("\n"));
		UserGdpr gdpr = objectMapper.readValue(userDataString, UserGdpr.class);
		assertEquals(authService.getCurrentUser().getDescription(), gdpr.getDescription());
		assertEquals(authService.getCurrentUser().getEmail(), gdpr.getEmail());
		assertEquals(authService.getCurrentUser().getFirstName(), gdpr.getFirstName());
		assertEquals(objectMapper.writeValueAsString(authService.getCurrentUser().getDates()),
				objectMapper.writeValueAsString(gdpr.getDates()));
		assertEquals(objectMapper.writeValueAsString(authService.getCurrentUser().getDonations()),
				objectMapper.writeValueAsString(gdpr.getDonations()));
		assertEquals(objectMapper.writeValueAsString(authService.getCurrentUser().getGender()),
				objectMapper.writeValueAsString(gdpr.getGender()));
		assertEquals(objectMapper.writeValueAsString(authService.getCurrentUser().getIntention()),
				objectMapper.writeValueAsString(gdpr.getIntention()));
		// TODO Comparing Sets
//		assertEquals(authService.getCurrentUser().getPreferedGenders(), gdpr.getPreferedGenders());
//		assertEquals(authService.getCurrentUser().getInterests(),
//				gdpr.getInterests());
//		assertEquals(authService.getCurrentUser().getMessageSent(),
//				gdpr.getMessageSent());

//		assertEquals(objectMapper.writeValueAsString(authService.getCurrentUser().getWebPush()),
//				objectMapper.writeValueAsString(gdpr.getWebPush()));
		assertEquals(authService.getCurrentUser().getLocationLatitude(), gdpr.getLocationLatitude());
		assertEquals(authService.getCurrentUser().getLocationLongitude(), gdpr.getLocationLongitude());
		assertEquals(authService.getCurrentUser().getNumberProfileViews(), gdpr.getNumberProfileViews());
		assertEquals(authService.getCurrentUser().getNumberSearches(), gdpr.getNumberSearches());
		assertEquals(authService.getCurrentUser().getPreferedMaxAge(), gdpr.getPreferedMaxAge());
		assertEquals(authService.getCurrentUser().getTotalDonations(), gdpr.getTotalDonations(), 0.001);
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
