package com.nonononoki.alovoa.entity.user;

import com.nonononoki.alovoa.entity.User;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@EqualsAndHashCode(exclude = "user")
@NoArgsConstructor
public class UserSettings {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    private User user;

    private boolean emailLike = true;

    private boolean emailChat = true;

    public UserSettings(User user) {
        this.user = user;
    }

}
