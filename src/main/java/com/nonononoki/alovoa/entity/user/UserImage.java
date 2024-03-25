package com.nonononoki.alovoa.entity.user;

import java.util.Date;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nonononoki.alovoa.entity.User;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class UserImage {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique=true)
	private UUID uuid;

	@JsonIgnore
	@ManyToOne
	private User user;

	@Column(columnDefinition="mediumtext")
	private String content;

	private Date date;
}
