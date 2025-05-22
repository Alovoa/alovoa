package com.nonononoki.alovoa.model;

import com.nonononoki.alovoa.entity.User;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BaseRegisterDto {
	
	private User user;
	private RegisterDto registerDto;
	
}