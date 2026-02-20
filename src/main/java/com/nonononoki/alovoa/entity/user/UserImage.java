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

	@Column(name = "s3_key")
	private String s3Key;

	private String binMime;

	@Column(name = "source_provider", length = 64)
	private String sourceProvider;

	@Column(name = "source_url", length = 1024)
	private String sourceUrl;

	@Column(name = "source_verified")
	private Boolean sourceVerified;

	private Date date;
}
