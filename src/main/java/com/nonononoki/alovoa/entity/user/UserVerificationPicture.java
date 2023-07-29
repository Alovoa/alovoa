package com.nonononoki.alovoa.entity.user;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nonononoki.alovoa.entity.User;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
public class UserVerificationPicture {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @JsonIgnore
    @ManyToOne
    private User user;

    @ManyToMany
    @JoinColumn
    private List<User> userYes;

    @ManyToMany
    @JoinColumn
    private List<User> userNo;

    @Column(columnDefinition = "mediumtext")
    private String data;

    private Date date;

    private boolean verifiedByAdmin = false;
}
