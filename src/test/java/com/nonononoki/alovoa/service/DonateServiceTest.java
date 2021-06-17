package com.nonononoki.alovoa.service;

import static org.mockito.ArgumentMatchers.any;

import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.DonationBmac;
import com.nonononoki.alovoa.model.DonationBmac.DonationBmacResponse;
import com.nonononoki.alovoa.model.DonationKofi;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.UserDonationRepository;
import com.nonononoki.alovoa.repo.UserRepository;

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

	@MockBean
	private AuthService authService;

	@MockBean
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

		Assert.assertEquals(0, user1.getTotalDonations(), doubleDelta);
		Assert.assertEquals(0, userDonationRepository.count());

		String donationString = "10.00";
		double donationAmount = Double.valueOf(donationString);

		DonationKofi donationKofi = new DonationKofi();
		donationKofi.setAmount(donationString);
		donationKofi.setMessage(user1.getEmail());
		donateService.donationReceivedKofi(donationKofi);

		Assert.assertEquals(donationAmount, user1.getTotalDonations(), doubleDelta);
		Assert.assertEquals(1, userDonationRepository.count());

		double donationAmount2 = 15;

		DonationBmac donationBmac = new DonationBmac();
		DonationBmacResponse bmacResponse = new DonationBmacResponse();
		bmacResponse.setSupporter_email(user1.getEmail());
		bmacResponse.setTotal_amount(donationAmount2);
		donationBmac.setResponse(bmacResponse);
		donateService.donationReceivedBmac(donationBmac);

		Assert.assertTrue(user1.getTotalDonations() < donationAmount + donationAmount2
				&& user1.getTotalDonations() > donationAmount);
		Assert.assertEquals(2, userDonationRepository.count());

	}

}
