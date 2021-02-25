package com.nonononoki.alovoa.service;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserDonation;
import com.nonononoki.alovoa.model.DonationDto;
import com.nonononoki.alovoa.model.DonationKofi;
import com.nonononoki.alovoa.repo.UserDonationRepository;
import com.nonononoki.alovoa.repo.UserRepository;

@Service
public class DonateService {

	private final int FILTER_RECENT = 1;
	private final int FILTER_AMOUNT = 2;
	
	@Autowired
	private UserDonationRepository userDonationRepo;

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private HttpServletRequest request;

	@Autowired
	private AuthService authService;

	@Autowired
	private TextEncryptorConverter textEncryptor;
	
	@Value("${app.ip.kofi}")
	private String kofiIp;

	@Value("${app.donate.users.max}")
	private int maxEntries;
	
	@Value("${spring.profiles.active}")
	private String profile;

	public List<DonationDto> filter(int filter) throws Exception {
		List<DonationDto> donationsToDtos = null;

		User currentUser = authService.getCurrentUser();

		if (filter == FILTER_RECENT) {
			donationsToDtos = DonationDto.donationsToDtos(userDonationRepo.findAllByOrderByDateDesc(), currentUser,
					textEncryptor, maxEntries);
		} else if (filter == FILTER_AMOUNT) {
			donationsToDtos = DonationDto.usersToDtos(userRepo.usersDonate(),
					currentUser, textEncryptor, maxEntries);
		} else {
			throw new Exception("");
		}

		return donationsToDtos;
	}

	public void donationReceivedKofi(DonationKofi donation) throws Exception {
		if (request.getRemoteAddr().trim().equals(kofiIp) || !profile.equals(Tools.PROD)) {
			User u = null;
			
			if(donation.getFrom_name() != null) {
				u = userRepo.findByEmail(donation.getFrom_name().toLowerCase());
			}
			
			if (u == null && donation.getMessage() != null) {
				u = userRepo.findByEmail(donation.getMessage().toLowerCase());
			}
			
			if (u != null) {
				double amount = Double.parseDouble(donation.getAmount());
				UserDonation userDonation = new UserDonation();
				userDonation.setAmount(amount);
				userDonation.setDate(new Date());
				userDonation.setUser(u);
				u.getDonations().add(userDonation);
				u.setTotalDonations(u.getTotalDonations() + amount);
				userRepo.save(u);
			}
		}
	}

}
