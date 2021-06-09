package com.nonononoki.alovoa.model;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class SearchDto {
	
	private List<UserDto> users;
	private String message;
	
	boolean global;
	boolean incompatible;
}
