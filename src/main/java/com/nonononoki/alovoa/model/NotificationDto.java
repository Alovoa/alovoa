package com.nonononoki.alovoa.model;

import java.util.Date;

import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.UserNotification;

import lombok.Data;

@Data
public class NotificationDto {

	private long id;
	
	private Date date;
	
	private UserDto userFromDto;

	public static NotificationDto notificationToNotificationDto(UserNotification n, User currentUser, TextEncryptorConverter textEncryptor)
			throws Exception {
		NotificationDto dto = new NotificationDto();
		dto.setDate(n.getCreationDate());
		dto.setId(n.getId());
		dto.setUserFromDto(UserDto.userToUserDto(n.getUserFrom(), currentUser, textEncryptor));
		return dto;
	}
}
