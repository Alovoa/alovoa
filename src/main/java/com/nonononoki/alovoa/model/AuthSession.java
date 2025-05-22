package com.nonononoki.alovoa.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthSession {
	private String email;
	private long id;
}
