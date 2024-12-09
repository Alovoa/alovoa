package com.nonononoki.alovoa.entity.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nonononoki.alovoa.entity.User;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode
@Entity
public class Gender {
	
	@Transient
	public static final long MALE = 1;
	@Transient
	public static final long FEMALE = 2;
	@Transient
	public static final long OTHER = 3;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Exclude
	private Long id;

	@JsonIgnore
	@ManyToMany
	@EqualsAndHashCode.Exclude
	private List<User> users;

	@JsonIgnore
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "gender")
	@EqualsAndHashCode.Exclude
	private List<User> user;

	private String text;
}
