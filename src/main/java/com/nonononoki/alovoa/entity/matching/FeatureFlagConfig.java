package com.nonononoki.alovoa.entity.matching;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "feature_flags", uniqueConstraints = {
        @UniqueConstraint(name = "uk_feature_flag", columnNames = {"flag_name", "segment_key"})
})
public class FeatureFlagConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flag_name", nullable = false, length = 120)
    private String flagName;

    @Column(name = "segment_key", nullable = false, length = 255)
    private String segmentKey = "*";

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "json_config", columnDefinition = "text")
    private String jsonConfig;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Date updatedAt;
}
