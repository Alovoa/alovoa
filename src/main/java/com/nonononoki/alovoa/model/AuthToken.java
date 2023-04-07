package com.nonononoki.alovoa.model;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import lombok.EqualsAndHashCode;
import lombok.Value;

@SuppressWarnings("serial")
@Value
@EqualsAndHashCode(callSuper = false)
public class AuthToken extends UsernamePasswordAuthenticationToken {

	private String username;
	private String password;
	private long captchaId;
	private String captchaText;

	public AuthToken(Object principal, Object credentials) {
		super(principal, credentials);
		this.username = null;
		this.password = null;
		this.captchaId = -1;
		this.captchaText = null;
	}

	public AuthToken(String username, String password, long captchaId, String captchaText) {
		super(null, null);
		this.username = username;
		this.password = password;
		this.captchaId = captchaId;
		this.captchaText = captchaText;
	}

}
