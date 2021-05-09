package com.nonononoki.alovoa.model;

import java.util.Date;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserWebPush;

import lombok.Data;

@Data
public class WebPushDto {
	
	private Long id;

	private User user;
	
	private Date date;
	
	private String publicKey;

	private String endPoint;
	
	private String auth;
	
	public static UserWebPush toEntity(WebPushDto dto) {
		UserWebPush wp = new UserWebPush();
		wp.setAuth(dto.getAuth());
		wp.setDate(dto.getDate());
		wp.setEndPoint(dto.getEndPoint());
		wp.setId(dto.getId());
		wp.setPublicKey(dto.getPublicKey());
		wp.setUser(dto.getUser());
		return wp;
	}
 	
}
