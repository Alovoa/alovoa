package com.nonononoki.alovoa.model;

import lombok.Data;

@Data
public class DonationBmac {
	
	private DonationBmacResponse response;
	
	@Data
	public static class DonationBmacResponse {
		String supporter_email;
		String supporter_name;
		String supporter_message;
		double total_amount;
	}
}

