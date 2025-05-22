package com.nonononoki.alovoa.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDeleteAccountDto {
	 
	private String tokenString;
	 
	private String email;
	
	private String captchaText;
	
	private long captchaId;
	
	private boolean confirm;
	
}
