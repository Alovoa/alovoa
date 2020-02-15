package com.nonononoki.alovoa.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.UserDonation;

import lombok.Data;

@Data
public class DonationDto {
	
	private long id;
	private Date date;
	private User user;
	private double amount;
	
	public static DonationDto donationToDto(UserDonation d) {
		DonationDto dto = new DonationDto();
		dto.setId(d.getId());
		dto.setDate(d.getDate());
		dto.setAmount(d.getAmount());
		dto.setUser(d.getUser());
		return dto;
	}
	
	public static DonationDto userToDto(User user) {
		DonationDto dto = new DonationDto();
		dto.setId(user.getId());
		dto.setAmount(user.getTotalDonations());
		dto.setUser(user);
		return dto;
	}
	
	public static List<DonationDto> donationsToDtos(List<UserDonation> donations) {
		List<DonationDto> dtos = new ArrayList<>();
		for(int i = 0; i < donations.size(); i++) {
			dtos.add(DonationDto.donationToDto(donations.get(i)));
		}
		return dtos;
	}
	
	public static List<DonationDto> usersToDtos(List<User> users) {
		List<DonationDto> dtos = new ArrayList<>();
		for(int i = 0; i < users.size(); i++) {
			dtos.add(DonationDto.userToDto(users.get(i)));
		}
		return dtos;
	}
}
