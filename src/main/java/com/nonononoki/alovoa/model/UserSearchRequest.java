package com.nonononoki.alovoa.model;

import com.nonononoki.alovoa.entity.user.Gender;
import lombok.Builder;
import lombok.Data;

import java.util.Collection;
import java.util.Date;

@Builder
@Data
public class UserSearchRequest {
	private double minLat;
	private double maxLat ;
	private double minLong;
	private double maxLong;
	private int age;
	private Gender preferedGender;
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
