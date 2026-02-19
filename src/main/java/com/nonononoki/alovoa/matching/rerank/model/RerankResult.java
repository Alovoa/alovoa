package com.nonononoki.alovoa.matching.rerank.model;

import com.nonononoki.alovoa.model.MatchRecommendationDto;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder
public class RerankResult {
    List<MatchRecommendationDto> ranked;
    Map<Long, ScoreTrace> scoreTraces;
    boolean rerankerApplied;
    String reason;
    String segmentKey;
    String variant;
}
