package com.nonononoki.alovoa.model;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AdminAccountDeleteDto {
	private String email;
}
