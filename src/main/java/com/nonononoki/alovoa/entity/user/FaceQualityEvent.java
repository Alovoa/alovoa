package com.nonononoki.alovoa.entity.user;

import com.nonononoki.alovoa.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "face_quality_event")
public class FaceQualityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "content_type", length = 40)
    private String contentType;

    @Column(name = "quality_score")
    private Double qualityScore;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "provider", length = 100)
    private String provider;

    @Column(name = "model_version", length = 120)
    private String modelVersion;

    @Column(name = "signal_json", columnDefinition = "MEDIUMTEXT")
    private String signalJson;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false)
    private Date createdAt;
}

