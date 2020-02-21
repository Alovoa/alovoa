package com.nonononoki.alovoa.entity;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import lombok.Data;

@Data
@Entity
public class UserDates {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	@OneToOne
	private User user;

	private Date activeDate;
	 
	private Date creationDate;

	private Date intentionChangeDate;
	
	private Date notificationDate;
	
	private Date notificationCheckedDate;
	
	private Date messageDate;
	
	private Date messageCheckedDate;
}
