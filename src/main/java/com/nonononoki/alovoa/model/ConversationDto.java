package com.nonononoki.alovoa.model;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.Conversation;
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
import java.util.UUID;

@Data
public class ConversationDto {

    private long id;
    private Date lastUpdated;
    private String userName;
    private String userProfilePicture;
    private MessageDto lastMessage;
    @Deprecated
    private String userIdEncoded;
    private UUID uuid;
    private boolean read;

    public static ConversationDto conversationToDto(Conversation c, User currentUser,
        TextEncryptorConverter textEncryptor, UserService userService)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidAlgorithmParameterException {
        ConversationDto dto = new ConversationDto();
        dto.setId(c.getId());
        dto.setLastUpdated(c.getLastUpdated());
        if (c.getMessages() != null && !c.getMessages().isEmpty()) {
            MessageDto msg = MessageDto.messageToDto(c.getMessages().get(c.getMessages().size() - 1), currentUser);
            dto.setLastMessage(msg);
        }
        User u = c.getPartner(currentUser);
        dto.setUserName(u.getFirstName());
        if (u.getProfilePicture() != null) {
            dto.setUserProfilePicture(u.getProfilePicture().getData());
        }
        dto.setUserIdEncoded(UserDto.encodeId(u.getId(), textEncryptor));
        dto.setUuid(Tools.getUserUUID(u, userService));
        return dto;
    }
}
