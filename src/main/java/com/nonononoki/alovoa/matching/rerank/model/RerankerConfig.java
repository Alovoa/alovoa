package com.nonononoki.alovoa.matching.rerank.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RerankerConfig {

    private double tau = 200.0;
    private double p = 1.0;
    private double kappa = 30.0;
    private double lambda = 2.0;
    private double sMin = 0.20;
    private double epsilon = 0.01;
    private double n0 = 200.0;
    private double baselineAttractiveness = 0.5;
    private boolean enableExploration = true;
    private boolean debugTrace = false;
    private String experimentKey = "reranker_v1";
    private int trafficPercent = 100;
}
