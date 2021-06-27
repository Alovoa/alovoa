package com.nonononoki.alovoa.model;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class InfoDto {
	private long numConfirmedUsers;
	private long numFemaleUser;
	private long numMaleUsers;
	private long numLikes;
	private long numMatches;
}
