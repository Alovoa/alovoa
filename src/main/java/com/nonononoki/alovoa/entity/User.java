package com.nonononoki.alovoa.entity;

import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nonononoki.alovoa.component.TextEncryptorConverter;

import lombok.Data;

@Component
@Data
@Entity
public class User {

	@JsonIgnore
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@JsonIgnore
	@Column(nullable = false, unique = true)
	@Convert(converter = TextEncryptorConverter.class)
	private String email;

	@JsonIgnore
	private String password;

	@Convert(converter = TextEncryptorConverter.class)
	private String firstName;

	private String donationUsername;

	private Date dateOfBirth;

	@Column(columnDefinition = "mediumtext")
	private String profilePicture;

	@ManyToMany
	@JoinTable(name = "user2interest")
	private List<UserInterest> interests;

	@OneToOne(cascade = CascadeType.REMOVE, orphanRemoval = true)
	@Convert(converter = TextEncryptorConverter.class)
	private Location lastLocation;

	@OneToOne(cascade = CascadeType.REMOVE, orphanRemoval = true)
	private UserIntention intention;

	@JsonIgnore
	@OneToOne(cascade = CascadeType.REMOVE, orphanRemoval = true)
	private UserRegisterToken registerToken;

	@JsonIgnore
	@OneToOne(cascade = CascadeType.REMOVE, orphanRemoval = true)
	private UserPasswordToken passwordToken;

	@JsonIgnore
	@OneToOne(cascade = CascadeType.REMOVE, orphanRemoval = true)
	private UserDeleteToken deleteToken;

	@OneToOne(cascade = CascadeType.REMOVE, orphanRemoval = true)
	private Gender gender;

	@ManyToMany
	@JoinTable(name = "user2genders")
	private Set<Gender> preferedGenders;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "user")
	private List<UserImage> images;

	@JsonIgnore
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "user")
	private List<UserDonation> donations;

	@JsonIgnore
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userFrom")
	private List<UserLike> likes;

	@JsonIgnore
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userTo")
	private List<UserLike> likedBy;

	@JsonIgnore
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userFrom")
	private List<Conversation> conversations;

	@JsonIgnore
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userTo")
	private List<Conversation> conversationsBy;

	@JsonIgnore
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userFrom")
	private List<Message> messageSent;

	@JsonIgnore
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userTo")
	private List<Message> messageReceived;

	@JsonIgnore
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userFrom")
	private List<UserNotification> notificationsFrom;

	@JsonIgnore
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userTo")
	private List<UserNotification> notifications;

	@JsonIgnore
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userFrom")
	private List<UserHide> hiddenUsers;

	@JsonIgnore
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userTo")
	private List<UserHide> hiddenByUsers;

	@JsonIgnore
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userFrom")
	private List<UserBlock> blockedUsers;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userTo")
	private List<UserBlock> blockedByUsers;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userFrom")
	private List<UserReport> reported;

	@JsonIgnore
	private boolean admin;
	@JsonIgnore
	private boolean confirmed;
	@JsonIgnore
	private boolean disabled;

	private Date activeDate;

	@JsonIgnore
	private Date creationDate;

	@JsonIgnore
	private Date intentionChangeDate;
}
