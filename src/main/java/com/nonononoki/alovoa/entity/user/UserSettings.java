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
    private User user;

    private boolean emailLike;

    private boolean emailMatch;

    private boolean emailChat;


    public UserSettings(){
        emailLike=false;
        emailMatch=false;
        emailChat=false;
    }

    public UserSettings(boolean emailLike, boolean emailMatch, boolean emailChat){
        this.emailLike=emailLike;
        this.emailMatch=emailMatch;
        this.emailChat=emailChat;
    }



}
