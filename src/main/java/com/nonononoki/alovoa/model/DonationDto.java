package com.nonononoki.alovoa.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserDonation;

import lombok.Data;

@Data
public class DonationDto {

	private long id;
	private Date date;
	private UserDto user;
	private double amount;

	public static DonationDto donationToDto(UserDonation d, User currentUser, TextEncryptorConverter textEncryptor)
			throws Exception {
		DonationDto dto = new DonationDto();
		dto.setId(d.getId());
		dto.setDate(d.getDate());
		dto.setAmount(d.getAmount());
		dto.setUser(UserDto.userToUserDto(d.getUser(), currentUser, textEncryptor));
		return dto;
	}

	public static DonationDto donationToDto(UserDonation d, User currentUser, TextEncryptorConverter textEncryptor,
			int mode) throws Exception {
		DonationDto dto = new DonationDto();
		dto.setId(d.getId());
		dto.setDate(d.getDate());
		dto.setAmount(d.getAmount());
		dto.setUser(UserDto.userToUserDto(d.getUser(), currentUser, textEncryptor, mode));
		return dto;
	}

	public static DonationDto userToDto(User user, User currentUser, TextEncryptorConverter textEncryptor)
			throws Exception {
		DonationDto dto = new DonationDto();
		dto.setId(user.getId());
		dto.setAmount(user.getTotalDonations());
		dto.setUser(UserDto.userToUserDto(user, currentUser, textEncryptor));
		return dto;
	}

	public static DonationDto userToDto(User user, User currentUser, TextEncryptorConverter textEncryptor, int mode)
			throws Exception {
		DonationDto dto = new DonationDto();
		dto.setId(user.getId());
		dto.setAmount(user.getTotalDonations());
		dto.setUser(UserDto.userToUserDto(user, currentUser, textEncryptor, mode));
		return dto;
	}

	public static List<DonationDto> donationsToDtos(List<UserDonation> donations, User currentUser,
			TextEncryptorConverter textEncryptor, int maxEntries) throws Exception {
		List<DonationDto> dtos = new ArrayList<>();
		for (UserDonation donation : donations) {
			if (donation.getUser().getId() != currentUser.getId()) {
				dtos.add(DonationDto.donationToDto(donation, currentUser, textEncryptor));
				
				if(dtos.size() >= maxEntries) {
					break;
				}
			}
		}
		return dtos;
	}

	public static List<DonationDto> usersToDtos(List<User> users, User currentUser,
			TextEncryptorConverter textEncryptor, int maxEntries) throws Exception {
		List<DonationDto> dtos = new ArrayList<>();
		for (User user : users) {
			if (user.getId() != currentUser.getId()) {
				dtos.add(DonationDto.userToDto(user, currentUser, textEncryptor));
				
				if(dtos.size() >= maxEntries) {
					break;
				}
			}
		}
		return dtos;
	}
}
