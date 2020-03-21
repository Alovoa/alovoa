package com.nonononoki.alovoa.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.nonononoki.alovoa.component.TextEncryptorConverter;

import lombok.Data;

@Data
@Entity
public class Message{
	
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	@ManyToOne
	private Conversation conversation;
	
	@ManyToOne
	private User userFrom;
	
	@ManyToOne
	private User userTo;
	
	@Convert(converter = TextEncryptorConverter.class)
	@Column(columnDefinition="mediumtext")
	private String content;
	
	private Date creationDate;
	
}