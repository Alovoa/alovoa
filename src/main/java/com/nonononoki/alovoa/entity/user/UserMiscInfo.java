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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@EqualsAndHashCode
@NoArgsConstructor
public class UserMiscInfo {
	
	@Transient
	public static final long DRUGS_TOBACCO_YES = 1;
	@Transient
	public static final long DRUGS_ALCOHOL_YES = 2;
	@Transient
	public static final long DRUGS_CANNABIS_YES = 3;
	@Transient
	public static final long DRUGS_OTHER_YES = 4;

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

	@Transient
	public static final long DRUGS_TOBACCO_NO = 31;
	@Transient
	public static final long DRUGS_ALCOHOL_NO = 32;
	@Transient
	public static final long DRUGS_CANNABIS_NO = 33;
	@Transient
	public static final long DRUGS_OTHER_NO = 34;

	@Transient
	public static final long DRUGS_TOBACCO_SOMETIMES = 41;
	@Transient
	public static final long DRUGS_ALCOHOL_SOMETIMES = 42;
	@Transient
	public static final long DRUGS_CANNABIS_SOMETIMES = 43;
	@Transient
	public static final long DRUGS_OTHER_SOMETIMES = 44;

	@Transient
	public static final long RELATIONSHIP_TYPE_MONOGAMOUS = 51;
	@Transient
	public static final long RELATIONSHIP_TYPE_POLYAMOROUS = 52;

	@Transient
	public static final long GENDER_IDENTITY_CIS = 61;
	@Transient
	public static final long GENDER_IDENTITY_TRANS = 62;

	@Transient
	public static final long POLITICS_MODERATE = 71;
	@Transient
	public static final long POLITICS_LEFT = 72;
	@Transient
	public static final long POLITICS_RIGHT = 73;

	@Transient
	public static final long RELIGION_NO = 81;
	@Transient
	public static final long RELIGION_YES = 82;

	@Transient
	public static final long FAMILY_WANT = 91;
	@Transient
	public static final long FAMILY_NOT_WANT = 92;
	@Transient
	public static final long FAMILY_NOT_UNSURE = 93;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Exclude
	private Long id;

	@JsonIgnore
	@ManyToMany
	@EqualsAndHashCode.Exclude
	private List<User> users;

	@Column(unique=true)
	private long value;

	public UserMiscInfo(long value) {
		this.value = value;
	}
}
