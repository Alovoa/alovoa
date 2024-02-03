package com.nonononoki.alovoa.model;

import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserNotification;
import com.nonononoki.alovoa.service.UserService;
import lombok.Data;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

@Data
public class NotificationDto {

    private long id;

    private Date date;

    private UserDto userFromDto;

    private String message;

    public static NotificationDto notificationToNotificationDto(UserNotification n, User currentUser, UserService userService,
                                                                TextEncryptorConverter textEncryptor, boolean ignoreIntention)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
        NotificationDto dto = new NotificationDto();
        dto.setDate(n.getDate());
        dto.setId(n.getId());
        dto.setMessage(n.getMessage());
        dto.setUserFromDto(
                UserDto.userToUserDto(UserDto.DtoBuilder.builder().ignoreIntention(ignoreIntention)
                        .currentUser(currentUser).user(n.getUserFrom()).textEncryptor(textEncryptor).userService(userService).build()));
        return dto;
    }
}
