package com.nonononoki.alovoa.entity.matching;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "user_visual_attractiveness", uniqueConstraints = {
        @UniqueConstraint(name = "uk_visual_attractiveness_user", columnNames = {"user_id"})
})
public class UserVisualAttractiveness {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "visual_score", nullable = false)
    private double visualScore;

    @Column(name = "confidence", nullable = false)
    private double confidence;

    @Column(name = "source_provider", nullable = false, length = 80)
    private String sourceProvider;

    @Column(name = "model_version", nullable = false, length = 80)
    private String modelVersion;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Date updatedAt;
}
