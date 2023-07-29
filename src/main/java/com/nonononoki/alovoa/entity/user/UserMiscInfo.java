package com.nonononoki.alovoa.entity.user;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Transient;

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
	
	@JsonIgnore
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@JsonIgnore
	@ManyToMany
	private List<User> users;

	@Column(unique=true)
	private long value;

}
