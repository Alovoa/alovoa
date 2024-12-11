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
	private int age;
	private Date minDateDob;
	private Date maxDateDob;
	private Collection<Long> intentionIds;
	private Collection<Long> likeIds;
	private Collection<Long> hideIds;
	private Collection<Long> blockIds;
	private Collection<Long> blockedByIds;
	private Collection<Long> genderIds;

	private Collection<Integer> miscInfos;
	private Collection<String> interests;
}
