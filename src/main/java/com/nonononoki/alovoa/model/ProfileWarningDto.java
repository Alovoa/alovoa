package com.nonononoki.alovoa.model;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ProfileWarningDto {
	private boolean hasWarning;
	private boolean noProfilePicture;
	private boolean noDescription;
	private boolean noIntention;
	private boolean noGender;
	private boolean noLocation;
}
