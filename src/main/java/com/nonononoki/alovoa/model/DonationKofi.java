package com.nonononoki.alovoa.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DonationKofi {
	private String email;
	private String message_id;
	private String message;
	private String timestamp;
	private String type;
	private String from_name;
	private String amount;
	private String url;
	private boolean is_public;
	private String kofi_transaction_id;
}
