package com.nonononoki.alovoa.matching.rerank.model;

import com.nonononoki.alovoa.model.MatchRecommendationDto;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RerankCandidate {
    Long candidateId;
    String candidateUuid;
    double baseScore; // normalized S_ij in [0,1]
    int originalPosition;
    MatchRecommendationDto payload;
}
