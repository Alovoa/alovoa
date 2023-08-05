package com.nonononoki.alovoa.entity.user;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nonononoki.alovoa.entity.User;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class UserMiscInfo {
	
	@Transient
	public static final long DRUGS_TOBACCO = 1;
	@Transient
	public static final long DRUGS_ALCOHOL = 2;
	@Transient
	public static final long DRUGS_CANNABIS = 3;
	@Transient
	public static final long DRUGS_OTHER = 4;
	
	@Transient
	public static final long RELATIONSHIP_SINGLE = 11;
	@Transient
	public static final long RELATIONSHIP_TAKEN = 12;
	@Transient
	public static final long RELATIONSHIP_OPEN = 13;
	@Transient
	public static final long RELATIONSHIP_OTHER = 14;
	
	@Transient
	public static final long KIDS_NO = 21;
	@Transient
	public static final long KIDS_YES = 22;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@JsonIgnore
	@ManyToMany
	private List<User> users;

	@Column(unique=true)
	private long value;

}
