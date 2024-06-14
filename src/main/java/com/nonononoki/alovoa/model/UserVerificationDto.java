package com.nonononoki.alovoa.model;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserProfilePicture;
import com.nonononoki.alovoa.entity.user.UserVerificationPicture;
import com.nonononoki.alovoa.service.UserService;
import lombok.Data;

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
        dto.setVerificationPicture(UserVerificationPicture.getPublicUrl(userService.getDomain(), user.getVerificationPicture().getUuid()));
        dto.setProfilePicture(UserProfilePicture.getPublicUrl(userService.getDomain(), user.getProfilePicture().getUuid()));
        dto.setUuid(Tools.getUserUUID(user, userService));
        return dto;
    }
}
