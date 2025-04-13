package com.nonononoki.alovoa.model;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import lombok.EqualsAndHashCode;
import lombok.Value;

@SuppressWarnings("serial")
@Value
@EqualsAndHashCode(callSuper = false)
public class AuthToken extends UsernamePasswordAuthenticationToken {

	String username;
	String password;
	long captchaId;
	String captchaText;

	public AuthToken(String username, String password, long captchaId, String captchaText) {
		super(null, null);
		this.username = username;
		this.password = password;
		this.captchaId = captchaId;
		this.captchaText = captchaText;
	}

}
