package com.nonononoki.alovoa;

import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.DonationKofi;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.UserDonationRepository;
import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.service.AuthService;
import com.nonononoki.alovoa.service.CaptchaService;
import com.nonononoki.alovoa.service.DonateService;
import com.nonononoki.alovoa.service.RegisterService;
import com.nonononoki.alovoa.service.UserService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DonateTest {

	@Autowired
	private RegisterService registerService;

	@Autowired
	private CaptchaService captchaService;
	
	@Autowired
	private UserDonationRepository userDonationRepository;
	
	@Value("${app.ip.kofi}")
	private String kofiIp;

	@Value("${app.age.min}")
	private int minAge;
	
	@Value("${app.message.size}")
	private int maxMessageSize;

	@MockBean
	private AuthService authService;
	
	@Autowired
	private DonateService donateService;
	
	@Autowired
	private UserService userService;

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private ConversationRepository conversationRepo;

	@Test
	public void test() throws Exception {
		
		List<User> testUsers = UserTest.getTestUsers(captchaService, registerService);
		User user1 = testUsers.get(1);
		
		double doubleDelta = 0.001;
		
		Assert.assertEquals(user1.getTotalDonations(), 0, doubleDelta);
		Assert.assertEquals(userDonationRepository.count(), 0);
		
		String donationString = "10.00";
		double donationAmount = Double.valueOf(donationString);

		DonationKofi donationKofi= new DonationKofi();
		donationKofi.setAmount(donationString);
		donationKofi.setMessage(user1.getEmail());
		donateService.donationReceivedKofi(donationKofi);
		
		Assert.assertEquals(user1.getTotalDonations(), donationAmount, doubleDelta);
		Assert.assertEquals(userDonationRepository.count(), 1);
		
		UserTest.deleteAllUsers(userService, authService, captchaService, conversationRepo, userRepo);
		
	}
	
 }
