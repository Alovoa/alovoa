package com.nonononoki.alovoa.entity.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.rest.MediaController;
import com.nonononoki.alovoa.service.UserService;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@EqualsAndHashCode
public class UserProfilePicture {

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private UUID uuid;

    @JsonIgnore
    @OneToOne
    @EqualsAndHashCode.Exclude
    private User user;

    @Deprecated
    @Column(columnDefinition = "mediumtext")
    @EqualsAndHashCode.Exclude
    private String data = null;

    @Lob
    @Column(length = 5000000)
    @EqualsAndHashCode.Exclude
    private byte[] bin;

    @EqualsAndHashCode.Exclude
    private String binMime;

    public static String getPublicUrl(String domain, UUID uuid) {
        return domain + MediaController.URL_REQUEST_MAPPING +
                MediaController.URL_PROFILE_PICTURE + uuid;
    }

}
