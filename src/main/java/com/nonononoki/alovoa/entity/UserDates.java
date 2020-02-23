package com.nonononoki.alovoa.entity;

import java.util.Date;

import javax.persistence.Column;
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
	
	@Column(nullable = false)
	private Date dateOfBirth;

	private Date activeDate  = new Date();
	private Date creationDate = new Date();
	private Date intentionChangeDate = new Date();
	private Date notificationDate = new Date();
	private Date notificationCheckedDate = new Date();
	private Date messageDate = new Date();
	private Date messageCheckedDate = new Date();
}
