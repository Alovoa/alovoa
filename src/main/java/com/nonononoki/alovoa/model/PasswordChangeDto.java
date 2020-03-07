package com.nonononoki.alovoa.model;

import lombok.Value;

@Value
public class PasswordChangeDto {

	private String email;
	private String password;
	private String token;
}
