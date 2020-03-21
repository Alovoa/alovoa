package com.nonononoki.alovoa.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.Conversation;
import com.nonononoki.alovoa.entity.User;

import lombok.Data;

@Data
public class ConversationDto {

	@JsonIgnore
	private long id;
	private Date lastUpdated;
	private String userName;
	private String userProfilePicture;
	private String lastMessage;
	private String userIdEncoded;
	private boolean read;
	
	public static ConversationDto conversationToDto(Conversation c, User currentUser, TextEncryptorConverter textEncryptor)
			throws Exception {
		ConversationDto dto = new ConversationDto();
		dto.setId(c.getId());
		dto.setLastUpdated(c.getLastUpdated());
		dto.setLastMessage(c.getLastMessage());
		User u = c.getUserFrom();
		if(u.equals(currentUser)) {
			u = c.getUserTo();
		}
		dto.setUserName(u.getFirstName());
		dto.setUserProfilePicture(u.getProfilePicture());
		dto.setUserIdEncoded(textEncryptor.encode(Long.toString(u.getId())));
		return dto;
	}
}
