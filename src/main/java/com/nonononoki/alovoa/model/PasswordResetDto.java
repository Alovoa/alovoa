package com.nonononoki.alovoa.model;

import lombok.Value;

@Value
public class PasswordResetDto {

	private long captchaId;
	private String captchaText;
	private String email;
}
