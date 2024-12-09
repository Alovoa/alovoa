package com.nonononoki.alovoa.entity.user;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
@Entity
public class Message {

	@JsonIgnore
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
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
	@Column(columnDefinition = "mediumtext", updatable = false)
	private String content;

	private Date date;

	private boolean allowedFormatting = false;

}