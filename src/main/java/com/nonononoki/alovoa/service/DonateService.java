package com.nonononoki.alovoa.service;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserDonation;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.DonationBmac;
import com.nonononoki.alovoa.model.DonationDto;
import com.nonononoki.alovoa.model.DonationKofi;
import com.nonononoki.alovoa.repo.UserDonationRepository;
import com.nonononoki.alovoa.repo.UserRepository;

@Service
public class DonateService {

	private static final int FILTER_RECENT = 1;
	private static final int FILTER_AMOUNT = 2;

	@Autowired
	private UserDonationRepository userDonationRepo;

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private HttpServletRequest request;

	@Autowired
	private AuthService authService;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private TextEncryptorConverter textEncryptor;

	@Value("${app.donate.users.max}")
	private int maxEntries;

	@Value("${spring.profiles.active}")
	private String profile;

	private static final Logger logger = LoggerFactory.getLogger(DonateService.class);

	private static final String KOFI_URL = "https://ko-fi.com/";
	private static final String KOFI_TEST_TRANSACTION_ID = "1234-1234-1234-1234";
	private static final String KOFI_TEST_EMAIL = "john@example.com";

	private static final String BMAC_URL = "https://www.buymeacoffee.com/";
	private static final String BMAC_TEST_EMAIL = "test@example.com";
	private static final double BMAC_AMOUNT_FACTOR = 0.95;

	public List<DonationDto> filter(int filter) throws AlovoaException, InvalidKeyException, IllegalBlockSizeException,
			BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
			UnsupportedEncodingException {
		List<DonationDto> donationsToDtos = null;

		User user = authService.getCurrentUser();
		
		int ageLegal = Tools.AGE_LEGAL;
		
		int age = Tools.calcUserAge(user);
		boolean isLegalAge = age >= ageLegal;
		int minAge = user.getPreferedMinAge();
		int maxAge = user.getPreferedMaxAge();

		if (isLegalAge && minAge < ageLegal) {
			minAge = ageLegal;
		}
		if (!isLegalAge && maxAge >= ageLegal) {
			maxAge = ageLegal - 1;
		}

		Date minDate = Tools.ageToDate(maxAge);
		Date maxDate = Tools.ageToDate(minAge);

		if (filter == FILTER_RECENT) {
			donationsToDtos = DonationDto.donationsToDtos(userDonationRepo.findTop20ByUserDatesDateOfBirthGreaterThanEqualAndUserDatesDateOfBirthLessThanEqualOrderByDateDesc(minDate, maxDate), user,
					textEncryptor, maxEntries);
		} else if (filter == FILTER_AMOUNT) {
			donationsToDtos = DonationDto.usersToDtos(userRepo.usersDonate(minDate, maxDate), user, textEncryptor, maxEntries);
		} else {
			throw new AlovoaException("filter_not_found");
		}

		return donationsToDtos;
	}

	public void donationReceivedKofi(DonationKofi donation) throws UnknownHostException, MalformedURLException {
		String kofiIp = InetAddress.getByName(new URL(KOFI_URL).getHost()).getHostAddress().trim();
		String ip = request.getRemoteAddr().trim();

		try {
			logger.info(objectMapper.writeValueAsString(donation));
		} catch (Exception e) {
			logger.error(e.getMessage());
		}

		if (kofiIp.equals(ip) || !profile.equals(Tools.PROD)) {

			Date now = new Date();

			if (profile.equals(Tools.PROD) && (KOFI_TEST_TRANSACTION_ID.equals(donation.getKofi_transaction_id())
					|| donation.getEmail() != null && KOFI_TEST_EMAIL.equalsIgnoreCase(donation.getEmail())
					|| !donation.is_public())) {
				return;
			}

			User u = null;

			if (donation.getFrom_name() != null) {
				u = userRepo.findByEmail(donation.getFrom_name().toLowerCase());
			}

			if (u == null && donation.getMessage() != null) {
				u = userRepo.findByEmail(donation.getMessage().toLowerCase());
			}

			// in case user forgot, check their Ko-fi email address just in case
			if (u == null && donation.getEmail() != null) {
				u = userRepo.findByEmail(donation.getEmail().toLowerCase());
			}

			if (u != null) {
				double amount = Double.parseDouble(donation.getAmount());
				UserDonation userDonation = new UserDonation();
				userDonation.setAmount(amount);
				userDonation.setDate(now);
				userDonation.setUser(u);
				u.getDonations().add(userDonation);
				u.setTotalDonations(u.getTotalDonations() + amount);
				u.getDates().setLatestDonationDate(new Date());
				userRepo.save(u);
			}
		}
	}

	public void donationReceivedBmac(DonationBmac data) throws UnknownHostException, MalformedURLException {
		String bmacIp = InetAddress.getByName(new URL(BMAC_URL).getHost()).getHostAddress().trim();
		String ip = request.getRemoteAddr().trim();
		
		try {
			logger.info(objectMapper.writeValueAsString(data));
		} catch (Exception e) {
			logger.error(e.getMessage());
		}

		if (bmacIp.equals(ip) || !profile.equals(Tools.PROD)) {

			Date now = new Date();
			DonationBmac.DonationBmacResponse donation = data.getResponse();

			if (profile.equals(Tools.PROD) && BMAC_TEST_EMAIL.equalsIgnoreCase(donation.getSupporter_email())) {
				return;
			}

			User u = null;

			if (donation.getSupporter_name() != null) {
				u = userRepo.findByEmail(donation.getSupporter_name().toLowerCase());
			}

			if (u == null && donation.getSupporter_message() != null) {
				u = userRepo.findByEmail(donation.getSupporter_message().toLowerCase());
			}

			if (u == null && donation.getSupporter_email() != null) {
				u = userRepo.findByEmail(donation.getSupporter_email().toLowerCase());
			}

			if (u != null) {
				UserDonation userDonation = new UserDonation();
				double amount = donation.getTotal_amount() * BMAC_AMOUNT_FACTOR;
				amount = (double) Math.round(amount * 100) / 100;
				userDonation.setAmount(amount);
				userDonation.setDate(now);
				userDonation.setUser(u);
				u.getDonations().add(userDonation);
				u.setTotalDonations(u.getTotalDonations() + amount);
				u.getDates().setLatestDonationDate(new Date());
				userRepo.save(u);
			}
		}
	}

}
