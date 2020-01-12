package com.nonononoki.alovoa.entity;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

import lombok.Data;

@Data
@Entity	
public class UserInterest {

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	private String text;
	
	@ManyToMany
	private List<User> users;
	
	@Override
    public boolean equals(Object o) { 
		
        if (o == this) { 
            return true; 
        } 
  
        if (!(o instanceof UserInterest)) { 
            return false; 
        } 
        
        UserInterest i = (UserInterest)o;
        if(i.getId() == id) {
        	return true;
        } else {
        	return false;
        }
    } 
	
    @Override
    public int hashCode() {
        return this.id.hashCode();
    }
}
