package com.nonononoki.alovoa.model;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class SearchDto {
	
	public enum SearchStage {
		NORMAL,
		INCREASED_RADIUS_1,
		INCREASED_RADIUS_2,
		WORLD,
		IGNORE_1,
		IGNORE_2,
		IGNORE_ALL
	}
	
	private List<UserDto> users;
	private String message;
	private SearchStage stage;
	
	boolean global;
	boolean incompatible;
	
}
