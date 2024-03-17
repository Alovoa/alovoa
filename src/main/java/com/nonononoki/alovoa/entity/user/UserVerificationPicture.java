package com.nonononoki.alovoa.entity.user;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nonononoki.alovoa.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class UserVerificationPicture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne
    private User user;

    @ManyToMany(mappedBy = "verificationYes")
    private List<User> userYes;

    @ManyToMany(mappedBy = "verificationNo")
    private List<User> userNo;

    @Column(columnDefinition = "mediumtext")
    private String data;

    private Date date;

    private boolean verifiedByAdmin = false;
}
