package com.nonononoki.alovoa.entity;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.nonononoki.alovoa.model.UserDto;

import lombok.Data;

@Data
@Entity
public class UserBlock {
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	@ManyToOne
	private User userFrom;
	
	@ManyToOne
	private User userTo;
	
	private Date date;
	
	@Override
    public boolean equals(Object o) { 	
        if (o == this) { 
            return true; 
        } 
  
        if (!(o instanceof UserDto)) { 
            return false; 
        } 
        
        UserDto i = (UserDto)o;
        if(i.getId() == id) {
        	return true;
        } else {
        	return false;
        }
    } 
	
    @Override
    public int hashCode() {
        return this.userTo.hashCode();
    }
}
