package com.nonononoki.alovoa.model;

import java.util.Date;

import lombok.Data;

@Data
public class RegisterDto {
	
	private String email;
	private String password;
	private String firstName;
	private Date dateOfBirth;
	private String referrerCode;
	
	private long gender;
	
	private boolean termsConditions;
	private boolean privacy;

	private String captchaText;
	private long captchaId;
}
