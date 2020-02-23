package com.nonononoki.alovoa.entity;

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
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.springframework.stereotype.Component;

import com.nonononoki.alovoa.component.TextEncryptorConverter;

import lombok.Data;

@Component
@Data
@Entity
public class User {
 
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Column(nullable = false, unique = true)
	@Convert(converter = TextEncryptorConverter.class)
	private String email;
	private String password;

	@Convert(converter = TextEncryptorConverter.class)
	private String firstName;

	private String description;

	private String donationUsername;

	private int preferedMinAge;

	private int preferedMaxAge;

	private int age;

	@Column(columnDefinition = "mediumtext")
	private String profilePicture;
	
	private double totalDonations;

	@ManyToMany
	@JoinTable(name = "user2interest")
	private List<UserInterest> interests;

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	private Location lastLocation;
	
	@OneToOne(cascade = CascadeType.REMOVE, orphanRemoval = true)
	private UserRegisterToken registerToken;

	@OneToOne(cascade = CascadeType.REMOVE, orphanRemoval = true)
	private UserPasswordToken passwordToken;
	 
	@OneToOne(cascade = CascadeType.REMOVE, orphanRemoval = true)
	private UserDeleteToken deleteToken;
	
	@OneToOne(cascade = CascadeType.REMOVE, orphanRemoval = true)
	private UserDates dates;

	@ManyToOne
	private Gender gender;

	@ManyToMany
	@JoinTable(name = "user2genders")
	private Set<Gender> preferedGenders;

	@ManyToOne
	private UserIntention intention;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "user")
	private List<UserImage> images;
	 
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "user")
	private List<UserDonation> donations;
	 
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userFrom")
	private List<UserLike> likes;
	 
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userTo")
	private List<UserLike> likedBy;
	 
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userFrom")
	private List<Conversation> conversations;
	 
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userTo")
	private List<Conversation> conversationsBy;
	 
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userFrom")
	private List<Message> messageSent;
	 
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userTo")
	private List<Message> messageReceived;
	 
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userFrom")
	private List<UserNotification> notificationsFrom;
	 
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userTo")
	private List<UserNotification> notifications;
	 
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userFrom")
	private List<UserHide> hiddenUsers;
	 
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userTo")
	private List<UserHide> hiddenByUsers;
	 
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userFrom")
	private List<UserBlock> blockedUsers;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userTo")
	private List<UserBlock> blockedByUsers;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userFrom")
	private List<UserReport> reported;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userTo")
	private List<UserReport> reportedByUsers;
	 
	private boolean admin;
	 
	private boolean confirmed;
	 
	private boolean disabled;

	//private Date activeDate;
	 
	//private Date creationDate;

	//private Date intentionChangeDate;
	
	//private Date notificationDate;
	
	//private Date notificationCheckedDate;
	
	//private Date messageDate;
	
	//private Date messageCheckedDate;
}
