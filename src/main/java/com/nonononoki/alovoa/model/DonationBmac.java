package com.nonononoki.alovoa.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DonationBmac {
	
	private DonationBmacResponse response;

	@Getter
	@Setter
	public static class DonationBmacResponse {
		String supporter_email;
		String supporter_name;
		String supporter_message;
		double total_amount;
	}
}

