package com.nonononoki.alovoa.entity.user;

import java.time.LocalDate;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nonononoki.alovoa.entity.User;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@EqualsAndHashCode(exclude="user")
public class UserDates {
	
	@JsonIgnore
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	@JsonIgnore
	@OneToOne
	private User user;
	
	@Column(nullable = false)
	//private Date dateOfBirth;
	private LocalDate dateOfBirth;

	private Date activeDate = new Date();
	private Date creationDate = new Date();
	private Date intentionChangeDate = new Date();
	private Date notificationDate = new Date();
	private Date notificationCheckedDate = new Date();
	private Date messageDate = new Date();
	private Date messageCheckedDate = new Date();
}
