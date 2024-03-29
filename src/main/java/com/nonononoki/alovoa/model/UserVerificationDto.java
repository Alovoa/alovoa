package com.nonononoki.alovoa.model;

import com.nonononoki.alovoa.Tools;
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
import java.util.UUID;

@Data
public class UserVerificationDto {
    private String verificationPicture;
    private String profilePicture;
    private String verificationString;
    private UUID uuid;

    public static UserVerificationDto map(User user, UserService userService) {
        UserVerificationDto dto = new UserVerificationDto();
        dto.setVerificationString(userService.getVerificationCode(user));
        dto.setVerificationPicture(user.getVerificationPicture().getData());
        dto.setProfilePicture(user.getProfilePicture().getData());
        dto.setUuid(Tools.getUserUUID(user, userService));
        return dto;
    }
}
