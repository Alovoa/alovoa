package com.nonononoki.alovoa.entity;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

@Data
@Entity
public class Captcha {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	
	@Column(columnDefinition="mediumtext")
	private String image;
	
	@JsonIgnore
	private String text;
	
	@JsonIgnore
	private Date date;
	
	@JsonIgnore
	@Column(unique=true, nullable=false)
	private String hashCode;
}
