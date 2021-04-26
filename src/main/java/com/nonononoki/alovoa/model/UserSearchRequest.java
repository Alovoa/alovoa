package com.nonononoki.alovoa.model;

import java.util.Collection;
import java.util.Date;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UserSearchRequest {
	private double minLat;
	private double maxLat ;
	private double minLong;
	private double maxLong;
	private Date minDate;
	private Date maxDate;
	private String intentionText;
	private Collection<Long> likeIds;
	private Collection<Long> hideIds;
	private Collection<Long> blockIds;
	private Collection<String> genderTexts;
}
