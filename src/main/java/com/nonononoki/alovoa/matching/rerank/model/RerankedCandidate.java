package com.nonononoki.alovoa.matching.rerank.model;

import com.nonononoki.alovoa.model.MatchRecommendationDto;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;

@Value
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class RerankedCandidate {
    MatchRecommendationDto recommendation;
    ScoreTrace scoreTrace;
    @ToString.Include
    int originalPosition;
}
