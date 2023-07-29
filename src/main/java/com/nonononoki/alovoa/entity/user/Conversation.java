package com.nonononoki.alovoa.entity.user;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.repo.UserBlockRepository;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Conversation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToMany
	private List<User> users;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "conversation")
	private List<Message> messages;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "conversation")
	private List<ConversationCheckedDate> checkedDates;

	private Date date;

	private Date lastUpdated;

	private Date lastOpened;

	public boolean containsUser(User user) {
		return users.contains(user);
	}

	public User getPartner(User user) {
		List<User> usersCopy = new ArrayList<>(users);
		usersCopy.remove(user);
		return usersCopy.get(0);
	}

	public boolean isBlocked(UserBlockRepository userBlockRepo) {

		UserBlock blockFrom = userBlockRepo.findByUserFromAndUserTo(users.get(0), users.get(1));
		UserBlock blockTo = userBlockRepo.findByUserFromAndUserTo(users.get(1), users.get(0));

		return blockFrom != null || blockTo != null;
	}

}