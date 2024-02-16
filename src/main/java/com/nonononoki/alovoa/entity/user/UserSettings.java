package com.nonononoki.alovoa.entity.user;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import com.nonononoki.alovoa.entity.User;

@Getter
@Setter
@Entity
@EqualsAndHashCode(exclude = "user")
public class UserSettings {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    private User user;

    private boolean emailLike;

    private boolean emailChat;

}
