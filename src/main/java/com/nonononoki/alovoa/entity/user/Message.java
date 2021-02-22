package com.nonononoki.alovoa.entity.user;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;

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
	@JoinColumn
	private User userFrom;
	
	@JsonIgnore
	@ManyToOne
	@JoinColumn
	private User userTo;
	
	@Convert(converter = TextEncryptorConverter.class)
	@Column(columnDefinition="mediumtext")
	private String content;
	
	private Date creationDate;
	
	private boolean allowedFormatting = false;
	
}