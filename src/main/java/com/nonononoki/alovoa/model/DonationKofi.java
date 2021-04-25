package com.nonononoki.alovoa.model;

import lombok.Data;

@Data
public class DonationKofi {
	String email;
	String message_id;
	String message;
	String timestamp;
	String type;
	String from_name;
	String amount;
	String url;
	boolean is_public;
	String kofi_transaction_id;
}
