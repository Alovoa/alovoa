package com.nonononoki.alovoa.entity.user;

import java.util.Date;
import java.util.UUID;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nonononoki.alovoa.entity.User;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class UserImage {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique=true)
	private UUID uuid;

	@JsonIgnore
	@ManyToOne
	private User user;

	@Deprecated
	@Column(columnDefinition="mediumtext")
	private String content;

	@Lob
	@Column(length=5000000)
	private byte[] bin;

	private String binMime;

	private Date date;
}
