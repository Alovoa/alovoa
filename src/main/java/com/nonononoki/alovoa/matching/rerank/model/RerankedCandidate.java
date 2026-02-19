package com.nonononoki.alovoa.matching.rerank.model;

import com.nonononoki.alovoa.model.MatchRecommendationDto;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RerankedCandidate {
    MatchRecommendationDto recommendation;
    ScoreTrace scoreTrace;
    int originalPosition;
}
