package com.nonononoki.alovoa.model;

import lombok.Data;

@Data
public class DonationKofi {
	String message_id;
	String message;
	String timestamp;
	String type;
	String from_name;
	String amount;
	String url;
}
