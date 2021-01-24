package com.nonononoki.alovoa.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nonononoki.alovoa.component.TextEncryptorConverter;

import lombok.Data;

@Data
@Entity
public class Message{
	
	@JsonIgnore
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	@JsonIgnore
	@ManyToOne
	private Conversation conversation;
	
	@JsonIgnore
	@ManyToOne
	private User userFrom;
	
	@JsonIgnore
	@ManyToOne
	private User userTo;
	
	@Convert(converter = TextEncryptorConverter.class)
	@Column(columnDefinition="mediumtext")
	private String content;
	
	private Date creationDate;
	
	private boolean allowedFormatting = false;
	
}