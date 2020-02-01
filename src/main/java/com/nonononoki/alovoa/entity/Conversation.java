package com.nonononoki.alovoa.entity;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import lombok.Data;

@Data
@Entity
public class Conversation {
	
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	@ManyToOne
	private User userFrom;

	@ManyToOne
	private User userTo;
	
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "conversation")
	private List<Message> messages;
	
	private Date creationDate;
	
	private Date lastUpdated;
	
}