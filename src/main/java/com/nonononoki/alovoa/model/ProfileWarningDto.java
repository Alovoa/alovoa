package com.nonononoki.alovoa.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfileWarningDto {
	private boolean hasWarning;
	private boolean noProfilePicture;
	private boolean noDescription;
	private boolean noIntention;
	private boolean noGender;
	private boolean noLocation;
}
