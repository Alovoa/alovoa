package com.nonononoki.alovoa.matching.rerank.model;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class ScoreTrace {
    double s;
    double fExposure;
    double fCapacity;
    double fGap;
    double fCollaborative;
    double ucb;
    double finalScore;
    String segment;
    int desirabilityDecile;
    Map<String, Object> windowStats;
}
