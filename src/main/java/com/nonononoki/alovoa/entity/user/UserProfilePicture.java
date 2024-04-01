package com.nonononoki.alovoa.entity.user;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nonononoki.alovoa.entity.User;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@EqualsAndHashCode(exclude="user")
public class UserProfilePicture {

	@JsonIgnore
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@JsonIgnore
	@OneToOne
	private User user;

	@Deprecated
	@Column(columnDefinition = "mediumtext")
	private String data = null;

	@Lob
	@Column(length=5000000)
	private byte[] bin;

	private String binMime;


}
