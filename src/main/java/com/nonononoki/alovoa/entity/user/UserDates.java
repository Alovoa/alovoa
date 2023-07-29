package com.nonononoki.alovoa.entity.user;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nonononoki.alovoa.entity.User;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@EqualsAndHashCode(exclude="user")
public class UserDates {
	
	@JsonIgnore
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@JsonIgnore
	@OneToOne
	private User user;
	
	@Column(nullable = false)
	private Date dateOfBirth;

	private Date activeDate = new Date();
	private Date creationDate = new Date();
	private Date intentionChangeDate = new Date();
	private Date notificationDate = new Date();
	private Date notificationCheckedDate = new Date();
	private Date messageDate = new Date();
	private Date messageCheckedDate = new Date();
	private Date latestDonationDate = new Date();
}
