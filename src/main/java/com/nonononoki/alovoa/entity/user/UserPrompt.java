package com.nonononoki.alovoa.entity.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nonononoki.alovoa.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class UserPrompt {

	@JsonIgnore
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@JsonIgnore
	@ManyToOne
	private User user;

	private Long promptId;

	private String text;

	@Override
	public boolean equals(Object o) {

		if (o == this) {
			return true;
		}

		if (!(o instanceof UserPrompt i)) {
			return false;
		}

        return i.getText().equals(text);
	}

	@Override
	public int hashCode() {
		return this.text.hashCode();
	}
}
