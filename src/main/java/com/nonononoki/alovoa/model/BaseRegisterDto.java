package com.nonononoki.alovoa.model;

import com.nonononoki.alovoa.entity.User;

import lombok.Data;

@Data
public class BaseRegisterDto {
	
	private User user;
	private RegisterDto registerDto;
	
}