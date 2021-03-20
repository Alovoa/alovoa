package com.nonononoki.alovoa.model;

import java.util.Collection;
import java.util.Date;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UserSearchRequest {
	double minLat;
	double maxLat ;
	double minLong;
	double maxLong;
	Date minDate;
	Date maxDate;
	String intentionText;
	Collection<Long> likeIds;
	Collection<Long> hideIds;
	Collection<Long> blockIds;
	Collection<String> genderTexts;
}
