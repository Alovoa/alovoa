package com.nonononoki.alovoa.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContactDto {
	private String email;
	private String message;
	private String captchaText;
	private long captchaId;	
}
