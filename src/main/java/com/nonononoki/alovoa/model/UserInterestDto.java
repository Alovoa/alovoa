package com.nonononoki.alovoa.model;

import lombok.Data;

@Data
public class UserInterestDto {
	
	public UserInterestDto (String name, long count) {
		this.name = name;
		this.count = count;
	}
	
	private String name;
	private long count;
	private String countString;
}
