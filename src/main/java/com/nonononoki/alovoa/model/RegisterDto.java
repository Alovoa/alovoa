package com.nonononoki.alovoa.model;

import java.util.Date;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterDto {
	
	private String email;
	private String password;
	private String firstName;
	private Date dateOfBirth;
	private String referrerCode;
	
	private long gender;
	
	private boolean termsConditions;
	private boolean privacy;
}
