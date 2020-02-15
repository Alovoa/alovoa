package com.nonononoki.alovoa.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.model.DonationDto;
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
	
}
