package com.nonononoki.alovoa.entity;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import com.nonononoki.alovoa.component.TextEncryptorConverter;

import lombok.Data;

@Data
@Entity
public class Contact {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	
	@Column(updatable = false)
	@Convert(converter = TextEncryptorConverter.class)
	private String email;
	
	@Convert(converter = TextEncryptorConverter.class)
	@Column(columnDefinition="mediumtext", updatable = false)
	private String message;
	
	private Date date;
	
	private boolean hidden;
}
