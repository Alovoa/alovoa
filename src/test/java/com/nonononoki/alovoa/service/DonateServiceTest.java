package com.nonononoki.alovoa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.DonationBmac;
import com.nonononoki.alovoa.model.DonationBmac.DonationBmacResponse;
import com.nonononoki.alovoa.model.DonationKofi;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.UserDonationRepository;
import com.nonononoki.alovoa.repo.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DonateServiceTest {

	@Autowired
	private RegisterService registerService;

	@Autowired
	private CaptchaService captchaService;

	@Autowired
	private UserDonationRepository userDonationRepository;

	@Value("${app.age.min}")
	private int minAge;

	@Value("${app.message.size}")
	private int maxMessageSize;

	@Value("${app.first-name.length-max}")
	private int firstNameLengthMax;

	@Value("${app.first-name.length-min}")
	private int firstNameLengthMin;
	
	@Value("${app.donate.kofi.key}")
	private String kofiKey;
	
	@Value("${app.donate.bmac.key}")
	private String bmacKey;

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private MailService mailService;

	@Autowired
	private DonateService donateService;

	@Autowired
	private UserService userService;

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private ConversationRepository conversationRepo;

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

		User user1 = testUsers.get(1);

		double doubleDelta = 0.001;

		assertEquals(0, user1.getTotalDonations(), doubleDelta);
		assertEquals(0, userDonationRepository.count());

		String donationString = "10.00";
		double donationAmount = Double.valueOf(donationString);

		DonationKofi donationKofi = new DonationKofi();
		donationKofi.setAmount(donationString);
		donationKofi.setMessage(user1.getEmail());
		donateService.donationReceivedKofi(donationKofi, kofiKey);

		assertEquals(donationAmount, user1.getTotalDonations(), doubleDelta);
		assertEquals(1, userDonationRepository.count());

		double donationAmount2 = 15;

		DonationBmac donationBmac = new DonationBmac();
		DonationBmacResponse bmacResponse = new DonationBmacResponse();
		bmacResponse.setSupporter_email(user1.getEmail());
		bmacResponse.setTotal_amount(donationAmount2);
		donationBmac.setResponse(bmacResponse);
		donateService.donationReceivedBmac(donationBmac, bmacKey);

		assertTrue(user1.getTotalDonations() == donationAmount + donationAmount2
				&& user1.getTotalDonations() > donationAmount);
		assertEquals(2, userDonationRepository.count());

	}

	@Test
	void testWithRealDataKofi() throws Exception {
		User user2 = testUsers.get(2);
		String kofiString = "{\"email\":\"" + user2.getEmail()
				+ "\",\"message_id\":\"27c5906e-8a77-4949-844c-a08d13f70340\",\"message\":\"" + user2.getEmail()
				+ "\",\"timestamp\":\"2022-06-26T08:24:19Z\",\"type\":\"Donation\",\"from_name\":\"Somebody\",\"amount\":\"3.00\",\"url\":\"https://ko-fi.com/Home/CoffeeShop?txid=e0397a56-594f-4764-8f80-761baafcdbf4&readToken=e274f1de-5fb3-4e38-86df-4af1de6bb3c9\",\"kofi_transaction_id\":\"e0397a56-594f-4764-8f80-761baafcdbf5\",\"_public\":false}";

		ObjectMapper objectMapper = new ObjectMapper();
		DonationKofi donationKofi = objectMapper.readValue(kofiString, DonationKofi.class);
		donateService.donationReceivedKofi(donationKofi, kofiKey);

		assertTrue(user2.getTotalDonations() > 0);
		assertEquals(1, userDonationRepository.count());
	}
}
