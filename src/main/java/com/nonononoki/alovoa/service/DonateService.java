package com.nonononoki.alovoa.service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.UserDonation;
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
	
	@Value("${app.donate.users.max}")
	private int maxEntries;

	public List<DonationDto> filter(int filter) throws Exception {
		List<DonationDto> donationsToDtos = null;
		
		if(filter == FILTER_RECENT) {
			donationsToDtos = DonationDto.donationsToDtos(userDonationRepo.findAllByOrderByDateDesc());
		} else if(filter == FILTER_AMOUNT) {
			donationsToDtos = DonationDto.usersToDtos(userRepo.findByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLastLocationNotNullOrderByTotalDonationsDesc());
		} else {
			throw new Exception("");
		}
		
		return donationsToDtos.stream().limit(maxEntries).collect(Collectors.toList());
	}
	
	public void donationReceivedKofi(DonationKofi d) {
		if(request.getRemoteAddr().trim().equals(IP_KOFI)) {	
			User u = null;
			u = userRepo.findByEmail(d.getFrom_name());
			if(u == null) {
				u = userRepo.findByEmail(d.getMessage());
			}
			if(u != null) {
				UserDonation userDonation = new UserDonation();
				userDonation.setAmount(Double.parseDouble(d.getAmount()));
				userDonation.setDate(new Date());
				userDonation.setUser(u);
				userDonationRepo.saveAndFlush(userDonation);
			}
		}
	}
	
}
