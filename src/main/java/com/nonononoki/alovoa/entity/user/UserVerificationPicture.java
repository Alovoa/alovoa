package com.nonononoki.alovoa.entity.user;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nonononoki.alovoa.entity.User;

import com.nonononoki.alovoa.rest.MediaController;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class UserVerificationPicture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique=true)
    private UUID uuid;

    @JsonIgnore
    @ManyToOne
    private User user;

    @ManyToMany(mappedBy = "verificationYes")
    private List<User> userYes;

    @ManyToMany(mappedBy = "verificationNo")
    private List<User> userNo;

    @Lob
    @Column(length=5000000)
    private byte[] bin;

    private String binMime;

    private Date date;

    private boolean verifiedByAdmin = false;

    public static String getPublicUrl(String domain, UUID uuid) {
        return domain + MediaController.URL_REQUEST_MAPPING +
                MediaController.URL_VERIFICATION_PICTURE + uuid;
    }
}
