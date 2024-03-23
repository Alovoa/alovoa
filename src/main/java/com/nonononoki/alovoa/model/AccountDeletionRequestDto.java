package com.nonononoki.alovoa.model;

import lombok.Data;

@Data
public class AccountDeletionRequestDto {
	private String email;
	private String captchaText;
	private long captchaId;
}
