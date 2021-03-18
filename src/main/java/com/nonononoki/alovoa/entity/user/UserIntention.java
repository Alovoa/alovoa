package com.nonononoki.alovoa.entity.user;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nonononoki.alovoa.entity.User;

import lombok.Data;

@Data
@Entity
public class UserIntention {
	
	@Transient
	public static int MEET = 0;
	@Transient
	public static int DATE = 1;
	@Transient
	public static int SEX = 2;

	@JsonIgnore
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	@JsonIgnore
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "intention")
	private List<User> user;
	
	private String text;
}
