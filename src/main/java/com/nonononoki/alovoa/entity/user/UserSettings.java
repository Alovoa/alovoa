package com.nonononoki.alovoa.entity.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import com.nonononoki.alovoa.entity.User;

@Getter
@Setter
@Entity
public class UserSettings {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    public User user;

    public boolean emailLike;

    public boolean emailMatch;

    public boolean emailChat;


    public UserSettings(){
        emailLike=true;
        emailMatch=true;
        emailChat=true;
    }

}
