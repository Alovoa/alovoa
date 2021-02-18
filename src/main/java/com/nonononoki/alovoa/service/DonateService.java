package com.nonononoki.alovoa.service;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

	private final String IP_KOFI = "104.45.229.87";

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

	@Value("${app.donate.users.max}")
	private int maxEntries;

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

	public void donationReceivedKofi(DonationKofi d) {
		if (request.getRemoteAddr().trim().equals(IP_KOFI)) {
			User u = null;
			u = userRepo.findByEmail(d.getFrom_name());
			if (u == null) {
				u = userRepo.findByEmail(d.getMessage());
			}
			if (u != null) {
				double amount = Double.parseDouble(d.getAmount());
				UserDonation userDonation = new UserDonation();
				userDonation.setAmount(amount);
				userDonation.setDate(new Date());
				userDonation.setUser(u);
				userDonationRepo.saveAndFlush(userDonation);

				u.setTotalDonations(u.getTotalDonations() + amount);
				userRepo.save(u);
			}
		}
	}

}
