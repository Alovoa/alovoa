package com.nonononoki.alovoa.matching.rerank.model;

import com.nonononoki.alovoa.model.MatchRecommendationDto;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;

@Value
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class RerankCandidate {
    @ToString.Include
    Long candidateId;
    @ToString.Include
    String candidateUuid;
    @ToString.Include
    double baseScore; // normalized S_ij in [0,1]
    @ToString.Include
    int originalPosition;
    MatchRecommendationDto payload;
}
