package com.nonononoki.alovoa.model;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class AdminAccountDeleteDto {
	private String email;
	private UUID uuid;
    private long id;
}
