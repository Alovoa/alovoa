package com.nonononoki.alovoa.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import com.nonononoki.alovoa.component.TextEncryptorConverter;

import lombok.Data;

@Data
@Entity
public class Contact {

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	@Convert(converter = TextEncryptorConverter.class)
	private String email;
	
	@Column(columnDefinition="mediumtext")
	private String message;
	
	private Date date;
}
