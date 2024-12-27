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

    public static DonationDto donationToDto(UserDonation d, User currentUser, UserService userService
            , TextEncryptorConverter textEncryptor, boolean ignoreIntention)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
        DonationDto dto = new DonationDto();
        dto.setId(d.getId());
        dto.setDate(d.getDate());
        dto.setAmount(d.getAmount());
        dto.setUser(UserDto.userToUserDto(UserDto.DtoBuilder.builder().ignoreIntention(ignoreIntention)
                .currentUser(currentUser).user(d.getUser()).userService(userService).build()));
        return dto;
    }

    public static DonationDto donationToDto(UserDonation d, User currentUser, UserService userService, TextEncryptorConverter textEncryptor,
                                            int mode, boolean ignoreIntention)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
        DonationDto dto = new DonationDto();
        dto.setId(d.getId());
        dto.setDate(d.getDate());
        dto.setAmount(d.getAmount());

        dto.setUser(UserDto.userToUserDto(UserDto.DtoBuilder.builder().ignoreIntention(ignoreIntention)
                .currentUser(currentUser).user(d.getUser()).userService(userService).build()));
        return dto;
    }

    public static DonationDto userToDto(User user, User currentUser, UserService userService,
                                        TextEncryptorConverter textEncryptor, boolean ignoreIntention)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
        DonationDto dto = new DonationDto();
        dto.setId(user.getId());
        dto.setAmount(user.getTotalDonations());
        dto.setUser(UserDto.userToUserDto(UserDto.DtoBuilder.builder().ignoreIntention(ignoreIntention)
                .currentUser(currentUser).user(user).userService(userService).build()));
        return dto;
    }

    public static List<DonationDto> donationsToDtos(List<UserDonation> donations, User currentUser, UserService userService,
                                                    TextEncryptorConverter textEncryptor, int maxEntries, boolean ignoreIntention) {
        return donations.subList(0, Math.min(donations.size(), maxEntries)).stream().map(donation -> {
            try {
                return DonationDto.donationToDto(donation, currentUser, userService, textEncryptor, ignoreIntention);
            } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException |
                     NoSuchPaddingException | InvalidAlgorithmParameterException | UnsupportedEncodingException |
                     AlovoaException e) {
                throw new RuntimeException(e);
            }
        }).toList();
    }

    public static List<DonationDto> usersToDtos(List<User> users, User currentUser, UserService userService,
                                                TextEncryptorConverter textEncryptor, int maxEntries, boolean ignoreIntention) {
        return users.subList(0, Math.min(users.size(), maxEntries)).stream().map(user -> {
            try {
                return DonationDto.userToDto(user, currentUser, userService, textEncryptor, ignoreIntention);
            } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException |
                     NoSuchPaddingException | InvalidAlgorithmParameterException | UnsupportedEncodingException |
                     AlovoaException e) {
                throw new RuntimeException(e);
            }
        }).toList();
    }
}
