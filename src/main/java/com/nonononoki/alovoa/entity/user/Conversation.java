package com.nonononoki.alovoa.entity.user;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.repo.UserBlockRepository;

import lombok.Getter;
import lombok.AccessLevel;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Conversation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToMany
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private List<User> users = new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "conversation")
	private List<Message> messages = new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "conversation")
	private List<ConversationCheckedDate> checkedDates;

	private Date date;

	private Date lastUpdated;

	private Date lastOpened;

	// Bridge to Real World tracking
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "first_message_date")
	private Date firstMessageDate;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "first_video_date_completed")
	private Date firstVideoDateCompleted;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "first_real_date_completed")
	private Date firstRealDateCompleted;

	@Column(name = "mutual_interest_confirmed")
	private Boolean mutualInterestConfirmed = false;

	@Column(name = "relationship_started_date")
	private LocalDate relationshipStartedDate;

	public boolean containsUser(User user) {
		return users.contains(user);
	}

	public List<User> getUsers() {
		return List.copyOf(users);
	}

	public void setUsers(List<User> users) {
		this.users = users == null ? new ArrayList<>() : new ArrayList<>(users);
	}

	public void addUser(User user) {
		if (user == null) {
			return;
		}
		if (!users.contains(user)) {
			users.add(user);
		}
	}

	public void removeUser(User user) {
		if (user == null) {
			return;
		}
		users.remove(user);
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
