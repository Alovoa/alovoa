package com.nonononoki.alovoa.entity.user;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;

import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.repo.UserBlockRepository;

import lombok.Data;

@Data
@Entity
public class Conversation {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
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

	@Convert(converter = TextEncryptorConverter.class)
	@Column(columnDefinition = "mediumtext")
	private String lastMessage;

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