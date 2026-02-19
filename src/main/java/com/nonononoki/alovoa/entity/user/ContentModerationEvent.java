package com.nonononoki.alovoa.entity.user;

import com.nonononoki.alovoa.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Entity
public class ContentModerationEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 30)
    private String contentType; // "MESSAGE", "PROFILE", "PROMPT_RESPONSE"

    private Double toxicityScore;

    @Column(length = 255)
    private String flaggedCategories;

    @Column(length = 100)
    private String provider;

    @Column(length = 120)
    private String modelVersion;

    @Column(length = 32)
    private String sourceMode;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String signalJson;

    @Column(nullable = false)
    private boolean blocked;

    @Column(nullable = false)
    private Date createdAt;
}
