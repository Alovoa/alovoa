package com.nonononoki.alovoa.model;

import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserDonation;
import com.nonononoki.alovoa.service.UserService;
import lombok.Data;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class DonationDto {

    private long id;
    private Date date;
    private UserDto user;
    private double amount;

    public static DonationDto donationToDto(UserDonation d, User currentUser, UserService userService, TextEncryptorConverter textEncryptor)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
        DonationDto dto = new DonationDto();
        dto.setId(d.getId());
        dto.setDate(d.getDate());
        dto.setAmount(d.getAmount());
        dto.setUser(UserDto.userToUserDto(d.getUser(), currentUser, userService, textEncryptor));
        return dto;
    }

    public static DonationDto donationToDto(UserDonation d, User currentUser, UserService userService, TextEncryptorConverter textEncryptor,
                                            int mode)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
        DonationDto dto = new DonationDto();
        dto.setId(d.getId());
        dto.setDate(d.getDate());
        dto.setAmount(d.getAmount());
        dto.setUser(UserDto.userToUserDto(d.getUser(), currentUser, userService, textEncryptor, mode));
        return dto;
    }

    public static DonationDto userToDto(User user, User currentUser, UserService userService, TextEncryptorConverter textEncryptor)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
        DonationDto dto = new DonationDto();
        dto.setId(user.getId());
        dto.setAmount(user.getTotalDonations());
        dto.setUser(UserDto.userToUserDto(user, currentUser, userService, textEncryptor));
        return dto;
    }

    public static DonationDto userToDto(User user, User currentUser, UserService userService, TextEncryptorConverter textEncryptor, int mode)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
        DonationDto dto = new DonationDto();
        dto.setId(user.getId());
        dto.setAmount(user.getTotalDonations());
        dto.setUser(UserDto.userToUserDto(user, currentUser, userService, textEncryptor, mode));
        return dto;
    }

    public static List<DonationDto> donationsToDtos(List<UserDonation> donations, User currentUser, UserService userService,
                                                    TextEncryptorConverter textEncryptor, int maxEntries)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
        List<DonationDto> dtos = new ArrayList<>();
        for (UserDonation donation : donations) {
            if (!donation.getUser().getId().equals(currentUser.getId())) {
                dtos.add(DonationDto.donationToDto(donation, currentUser, userService, textEncryptor));

                if (dtos.size() >= maxEntries) {
                    break;
                }
            }
        }
        return dtos;
    }

    public static List<DonationDto> usersToDtos(List<User> users, User currentUser, UserService userService,
                                                TextEncryptorConverter textEncryptor, int maxEntries)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
        List<DonationDto> dtos = new ArrayList<>();
        for (User user : users) {
            if (!user.getId().equals(currentUser.getId())) {
                dtos.add(DonationDto.userToDto(user, currentUser, userService, textEncryptor));

                if (dtos.size() >= maxEntries) {
                    break;
                }
            }
        }
        return dtos;
    }
}
