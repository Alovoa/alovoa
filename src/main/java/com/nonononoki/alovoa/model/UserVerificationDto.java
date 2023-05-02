package com.nonononoki.alovoa.model;

import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.service.UserService;
import lombok.Data;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Data
public class UserVerificationDto {
    private String verificationPicture;
    private String profilePicture;
    private String verificationString;
    private String idEnc;

    public static UserVerificationDto map(User user, UserService userService, TextEncryptorConverter textEncryptor)
            throws AlovoaException, InvalidAlgorithmParameterException, IllegalBlockSizeException, NoSuchPaddingException,
            UnsupportedEncodingException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        UserVerificationDto dto = new UserVerificationDto();
        dto.setVerificationString(userService.getVerificationCode(user));
        dto.setVerificationPicture(user.getVerificationPicture().getData());
        dto.setProfilePicture(user.getProfilePicture().getData());
        dto.setIdEnc(UserDto.encodeId(user.getId(), textEncryptor));
        return dto;
    }
}
