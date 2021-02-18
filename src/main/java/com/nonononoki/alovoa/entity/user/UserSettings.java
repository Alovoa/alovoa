package com.nonononoki.alovoa.entity.user;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import com.nonononoki.alovoa.entity.User;

import lombok.Data;

@Data
@Entity
public class UserSettings {
	
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	@OneToOne
	private User user;
	
	private boolean darkTheme;

}
