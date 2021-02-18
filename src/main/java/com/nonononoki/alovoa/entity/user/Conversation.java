package com.nonononoki.alovoa.entity.user;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.repo.UserBlockRepository;

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
	
	private Date lastCheckedFrom;
	
	private Date lastCheckedTo;
	
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
	
	public boolean isBlocked(UserBlockRepository userBlockRepo) {
		
		UserBlock blockFrom = userBlockRepo.findByUserFromAndUserTo(userFrom, userTo);
		UserBlock blockTo = userBlockRepo.findByUserFromAndUserTo(userTo, userFrom);
		
		if(blockFrom == null && blockTo == null) {
			return false;
		} else {
			return true;
		}
	}
	
}