package com.nonononoki.alovoa.entity.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nonononoki.alovoa.entity.User;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@EqualsAndHashCode
@NoArgsConstructor
public class UserSettings {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne
    @EqualsAndHashCode.Exclude
    private User user;

    private boolean emailLike = false;

    private boolean emailChat = false;

    // Growth-data controls (user-controlled, transparent, optional)
    private boolean shareGrowthProfile = true;

    private boolean allowBehaviorSignals = true;

    private boolean monthlyGrowthCheckins = true;

    public UserSettings(User user) {
        this.user = user;
    }

}
