package com.nonononoki.alovoa.entity.user;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nonononoki.alovoa.entity.User;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
		return i.getText().equals(text);
	}

	@Override
	public int hashCode() {
		return this.text.hashCode();
	}
}
