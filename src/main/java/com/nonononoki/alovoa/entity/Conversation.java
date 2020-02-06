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
	
	private Date lastOpened;
	
	private String lastMessage;
	
	public boolean containsUser(User user) {
		if(!getUserFrom().getId().equals(user.getId()) && !getUserTo().getId().equals(user.getId())) {
			return false;
		}	
		return true;
	}
	
	public User getPartner(User user) {
		User u = getUserFrom();
		if(u.equals(user)) {
			u = getUserTo();
		}		
		return u;
	}
	
}