package com.nonononoki.alovoa.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

@Data
@Entity
public class UserInterest {

	@JsonIgnore
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	private String text;

	@JsonIgnore
	@ManyToOne
	private User user;

	@Override
	public boolean equals(Object o) {

		if (o == this) {
			return true;
		}

		if (!(o instanceof UserInterest)) {
			return false;
		}

		UserInterest i = (UserInterest) o;
		if (i.getText().equals(text)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.text.hashCode();
	}
}
