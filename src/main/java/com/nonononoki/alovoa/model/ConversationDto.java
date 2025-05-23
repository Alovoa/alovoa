package com.nonononoki.alovoa.model;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.Conversation;
import com.nonononoki.alovoa.entity.user.UserProfilePicture;
import com.nonononoki.alovoa.service.UserService;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
public class ConversationDto {

    private long id;
    private Date lastUpdated;
    private String userName;
    private String userProfilePicture;
    private MessageDto lastMessage;
    private UUID uuid;
    private boolean read;

    public static ConversationDto conversationToDto(Conversation c, User currentUser, UserService userService) {
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
            dto.setUserProfilePicture(UserProfilePicture.getPublicUrl(userService.getDomain(),
                    Tools.getProfilePictureUUID(u.getProfilePicture(), userService)));
        }
        dto.setUuid(Tools.getUserUUID(u, userService));
        return dto;
    }
}
